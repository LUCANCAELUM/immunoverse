package com.example.data.repo

import android.util.Log
import com.example.data.kb.AcademyKb
import com.example.data.kb.DiseaseKb
import com.example.data.kb.ImmunologyKb
import com.example.data.network.DoiVerification
import com.example.data.network.LiteraturePaper
import com.example.data.network.ScientificApiClient
import com.example.data.network.WebSearchHit
import com.example.domain.VirtualLabEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

enum class AiExecutionMode(val label: String, val badge: String) {
    TURBO_RACE("Turbo Multi-Model", "Fastest + Search"),
    ENSEMBLE_COMPARE("Compare All Models", "Multi-Model + Search"),
    INSTANT_LITE("Instant Flash-Lite", "Ultra-Fast")
}

data class UploadedDoc(
    val id: String,
    val name: String,
    val content: String
)

data class RagChunk(
    val source: String,
    val title: String,
    val status: String,
    val text: String,
    val score: Double
)

data class ModelAnswerTrace(
    val modelLabel: String,
    val modelId: String,
    val text: String,
    val latencyMs: Long,
    val status: String // "Streaming", "Done", "Error"
)

data class AiStreamEvent(
    val partialText: String,
    val activeModelLabel: String = "Multi-Model",
    val done: Boolean = false,
    val ragChunks: List<RagChunk> = emptyList(),
    val webHits: List<WebSearchHit> = emptyList(),
    val livePapers: List<LiteraturePaper> = emptyList(),
    val verifiedDois: List<DoiVerification> = emptyList(),
    val modelTraces: List<ModelAnswerTrace> = emptyList(),
    val googleSearchUrl: String? = null,
    val error: String? = null
)

class ImmunoAiRepository {
    private val systemPrompt = """
        You are Immuno AI, a fast, rigorous scientific assistant in ImmunoVerse specializing in immunology, virology, microbiology, molecular biology, genomics, and biomedical research.
        RULES:
        1. Be concise, well-structured, and evidence-tagged ([Established], [Experimental], [Hypothesis], [Model-generated interpretation]).
        2. Ground answers in the retrieved Knowledge Base context, uploaded files, and live web/Europe PMC/OpenAlex search hits provided.
        3. NEVER fabricate DOIs or PMIDs; only cite DOIs explicitly present in the provided context.
        4. Never give actionable instructions for enhancing, engineering, or weaponizing pathogens, and never present answers as clinical patient diagnosis.
    """.trimIndent()

    private val liteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = content { text(systemPrompt) }
        )
    }

    private val flashModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) }
        )
    }

    private val argonModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            systemInstruction = content { text(systemPrompt) }
        )
    }

    private fun buildCorpus(uploadedDocs: List<UploadedDoc>): List<RagChunk> {
        val list = mutableListOf<RagChunk>()
        ImmunologyKb.CELLS.forEach { c ->
            list += RagChunk("Curated KB (Abbas/Janeway)", c.name, "Established", "${c.morphology} Function/Dev: ${c.development}. Activation: ${c.activation}. Markers: ${c.markers.joinToString()}. Clinical: ${c.clinical}. ${c.levels["Advanced"].orEmpty()}", 0.0)
        }
        ImmunologyKb.ORGANS.forEach { o ->
            list += RagChunk("Curated KB (Lymphoid Anatomy)", o.name, "Established", "${o.summary} ${o.functionText} Clinical: ${o.clinicalNote}", 0.0)
        }
        ImmunologyKb.MOLS.forEach { m ->
            list += RagChunk("Curated KB (Molecular)", "${m.name} (${m.pdbId ?: "No PDB"})", m.status, "${m.summary} Domains: ${m.domains} Ligand site: ${m.ligandSite}", 0.0)
        }
        ImmunologyKb.PATHWAYS.forEach { p ->
            list += RagChunk("Curated KB (Signalling)", p.name, "Established", "${p.summary} Nodes: ${p.nodes.joinToString { it.label }}. Outcome: ${p.outcome}", 0.0)
        }
        DiseaseKb.DISEASES.forEach { d ->
            list += RagChunk("Curated KB (Pathology)", d.name, "Established", "${d.overview} Agent: ${d.agent}. Immunopathology: ${d.immunoMechanism}. Treatment: ${d.treatment}. Research: ${d.research}", 0.0)
        }
        DiseaseKb.HIV_TOPICS.forEach { h ->
            list += RagChunk("HIV Immunology Research", h.title, h.status, h.summary, 0.0)
        }
        AcademyKb.TRACKS.forEach { t ->
            t.lessons.forEach { l ->
                list += RagChunk("Academy (${t.title})", l.title, "Established", "${l.summary} ${l.keyPoints.joinToString(" ")}", 0.0)
            }
        }
        VirtualLabEngine.EXPERIMENTS.forEach { e ->
            list += RagChunk("Virtual Lab (${e.category})", e.title, "Simulated/Method", "${e.objective} ${e.theory} Limitations: ${e.limitations}", 0.0)
        }
        uploadedDocs.forEach { doc ->
            val chunks = doc.content.chunked(1200).take(6)
            chunks.forEachIndexed { idx, piece ->
                list += RagChunk("User Upload (${doc.name})", "${doc.name} [Part ${idx + 1}]", "User Document", piece, 0.0)
            }
        }
        return list
    }

    fun retrieveContext(query: String, uploadedDocs: List<UploadedDoc>, topK: Int = 5): List<RagChunk> {
        val tokens = query.lowercase()
            .split(Regex("[^a-z0-9+]+"))
            .filter { it.length >= 3 }
            .distinct()
        if (tokens.isEmpty()) return emptyList()
        val corpus = buildCorpus(uploadedDocs)
        return corpus.map { chunk ->
            val hay = "${chunk.title} ${chunk.text}".lowercase()
            var score = 0.0
            for (tok in tokens) {
                if (chunk.title.lowercase().contains(tok)) score += 3.0
                if (hay.contains(tok)) score += 1.2
            }
            if (chunk.source.startsWith("User Upload") && score > 0) score += 1.5
            chunk.copy(score = score)
        }
            .filter { it.score > 0.5 }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun buildPrompt(
        userQuestion: String,
        ragHits: List<RagChunk>,
        webHits: List<WebSearchHit>,
        livePapers: List<LiteraturePaper>
    ): String = buildString {
        appendLine("=== USER QUESTION ===")
        appendLine(userQuestion)
        appendLine()
        if (ragHits.isNotEmpty()) {
            appendLine("=== IMMUNOVERSE KNOWLEDGE & USER DOCUMENTS ===")
            ragHits.forEachIndexed { idx, hit ->
                appendLine("[KB-${idx + 1}] (${hit.source} | ${hit.status}) ${hit.title}: ${hit.text}")
            }
            appendLine()
        }
        if (webHits.isNotEmpty()) {
            appendLine("=== LIVE WEB / ENCYCLOPEDIA SEARCH SNIPPETS ===")
            webHits.forEachIndexed { idx, w ->
                appendLine("[Web-${idx + 1}] (${w.engine}) ${w.title}: ${w.snippet}")
            }
            appendLine()
        }
        if (livePapers.isNotEmpty()) {
            appendLine("=== LIVE BIOMEDICAL LITERATURE (EUROPE PMC / OPENALEX) ===")
            livePapers.forEachIndexed { idx, p ->
                appendLine("[Ref-${idx + 1}] ${p.title} (${p.journal} ${p.year}) DOI: ${p.doi ?: "N/A"}")
            }
            appendLine()
        }
        appendLine("Provide a fast, clear, well-structured scientific answer with evidence tags ([Established], [Experimental], [Hypothesis], [Model-generated interpretation]).")
    }

    private fun buildOfflineSearchFallback(
        userQuestion: String,
        ragHits: List<RagChunk>,
        webHits: List<WebSearchHit>,
        livePapers: List<LiteraturePaper>,
        aiDiagnostics: String
    ): String = buildString {
        appendLine("[Local KB + Search Engine Synthesis] (Cloud AI models unavailable: $aiDiagnostics)")
        appendLine()
        if (ragHits.isNotEmpty()) {
            appendLine("Curated Knowledge Base Matches:")
            ragHits.take(4).forEach { hit ->
                appendLine("• [${hit.status}] ${hit.title} (${hit.source}): ${hit.text.take(280)}")
            }
            appendLine()
        }
        if (webHits.isNotEmpty()) {
            appendLine("Live Search Engine Findings:")
            webHits.take(4).forEach { w ->
                appendLine("• [${w.engine}] ${w.title}: ${w.snippet.take(240)}")
            }
            appendLine()
        }
        if (livePapers.isNotEmpty()) {
            appendLine("Indexed Research Articles:")
            livePapers.take(4).forEach { p ->
                appendLine("• ${p.title} (${p.journal}, ${p.year}) ${p.doi?.let { "DOI:$it" } ?: ""}")
            }
        }
        if (ragHits.isEmpty() && webHits.isEmpty() && livePapers.isEmpty()) {
            appendLine("No local KB matches found for \"$userQuestion\". Check internet connectivity or tap 'Open in Google Search' below.")
        }
    }

    fun askStream(
        userQuestion: String,
        uploadedDocs: List<UploadedDoc>,
        fetchLiveCitations: Boolean,
        mode: AiExecutionMode = AiExecutionMode.TURBO_RACE
    ): Flow<AiStreamEvent> = channelFlow {
        val ragHits = retrieveContext(userQuestion, uploadedDocs)
        val googleUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(userQuestion, "UTF-8")}"
        val mutex = Mutex()

        var webHits: List<WebSearchHit> = emptyList()
        var livePapers: List<LiteraturePaper> = emptyList()
        val traces = linkedMapOf<String, ModelAnswerTrace>()
        var primaryModelId = ""
        var primaryText = ""
        var activeLabel = "Initializing..."

        suspend fun emitSnapshot(done: Boolean = false, error: String? = null, dois: List<DoiVerification> = emptyList()) {
            mutex.withLock {
                send(
                    AiStreamEvent(
                        partialText = primaryText,
                        activeModelLabel = activeLabel,
                        done = done,
                        ragChunks = ragHits,
                        webHits = webHits,
                        livePapers = livePapers,
                        verifiedDois = dois,
                        modelTraces = traces.values.toList(),
                        googleSearchUrl = googleUrl,
                        error = error
                    )
                )
            }
        }

        // Immediately emit local KB hit status at T~5ms so UI never feels frozen
        emitSnapshot(done = false)

        // Launch parallel Web + Literature search (bounded strictly to <=3.5s, NEVER blocks Flash-Lite!)
        val searchDeferred = async {
            if (fetchLiveCitations) {
                withTimeoutOrNull(3600L) { ScientificApiClient.fastSearchWebAndLiterature(userQuestion) }
            } else null
        }

        val searchJob = launch {
            val bundle = searchDeferred.await()
            if (bundle != null) {
                mutex.withLock {
                    webHits = bundle.webHits
                    livePapers = bundle.papers
                }
                emitSnapshot(done = false)
            }
        }

        suspend fun streamOneModel(
            modelId: String,
            label: String,
            genModel: GenerativeModel,
            promptText: String,
            timeoutMs: Long,
            canPromoteToPrimary: Boolean,
            preferOverLite: Boolean = false
        ): ModelAnswerTrace {
            val startMs = System.currentTimeMillis()
            val acc = StringBuilder()
            mutex.withLock {
                traces[modelId] = ModelAnswerTrace(label, modelId, "", 0L, "Streaming")
            }
            return try {
                withTimeout(timeoutMs) {
                    genModel.generateContentStream(promptText).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            acc.append(piece)
                            val elapsed = System.currentTimeMillis() - startMs
                            mutex.withLock {
                                traces[modelId] = ModelAnswerTrace(label, modelId, acc.toString(), elapsed, "Streaming")
                                val shouldTakeOver = canPromoteToPrimary && (
                                    primaryModelId.isEmpty() ||
                                        primaryModelId == modelId ||
                                        (preferOverLite && primaryModelId == "gemini-3.5-flash-lite" && acc.length > 140)
                                    )
                                if (shouldTakeOver) {
                                    primaryModelId = modelId
                                    primaryText = acc.toString()
                                    activeLabel = "$label (${elapsed} ms)"
                                }
                            }
                            emitSnapshot(done = false)
                        }
                    }
                }
                val finalMs = System.currentTimeMillis() - startMs
                val finalTxt = acc.toString()
                val trace = if (finalTxt.isBlank()) {
                    ModelAnswerTrace(label, modelId, "", finalMs, "Empty response")
                } else {
                    ModelAnswerTrace(label, modelId, finalTxt, finalMs, "Done")
                }
                mutex.withLock {
                    traces[modelId] = trace
                    if (finalTxt.isNotBlank() && (primaryText.isBlank() || (preferOverLite && finalTxt.length >= 80))) {
                        primaryModelId = modelId
                        primaryText = finalTxt
                        activeLabel = "$label (${finalMs} ms)"
                    }
                }
                emitSnapshot(done = false)
                trace
            } catch (e: TimeoutCancellationException) {
                val elapsed = System.currentTimeMillis() - startMs
                val errTrace = ModelAnswerTrace(label, modelId, acc.toString(), elapsed, "Timed out")
                mutex.withLock { traces[modelId] = errTrace }
                emitSnapshot(done = false)
                errTrace
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ImmunoAI", "Model $modelId failed", e)
                val elapsed = System.currentTimeMillis() - startMs
                val errTrace = ModelAnswerTrace(label, modelId, acc.toString(), elapsed, e.localizedMessage ?: "Error")
                mutex.withLock { traces[modelId] = errTrace }
                emitSnapshot(done = false)
                errTrace
            }
        }

        val immediatePrompt = buildPrompt(userQuestion, ragHits, emptyList(), emptyList())

        when (mode) {
            AiExecutionMode.INSTANT_LITE -> {
                streamOneModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", liteModel, immediatePrompt, 22_000L, true)
                searchJob.join()
            }

            AiExecutionMode.TURBO_RACE -> coroutineScope {
                // 1. Start ultra-fast Flash-Lite at T=0ms immediately!
                val liteJob = async {
                    streamOneModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", liteModel, immediatePrompt, 20_000L, true)
                }
                // 2. Concurrently await fast web/literature search (<=2.2s soft wait) then launch Gemini 3.8 Flash grounded on live search!
                val flashJob = async {
                    withTimeoutOrNull(2200L) { searchJob.join() }
                    val enrichedPrompt = buildPrompt(userQuestion, ragHits, webHits, livePapers)
                    streamOneModel(
                        modelId = "gemini-3.8-flash",
                        label = "Gemini 3.8 Flash + Search",
                        genModel = flashModel,
                        promptText = enrichedPrompt,
                        timeoutMs = 28_000L,
                        canPromoteToPrimary = true,
                        preferOverLite = true
                    )
                }
                awaitAll(liteJob, flashJob)
                searchJob.join()
            }

            AiExecutionMode.ENSEMBLE_COMPARE -> coroutineScope {
                withTimeoutOrNull(2400L) { searchJob.join() }
                val enrichedPrompt = buildPrompt(userQuestion, ragHits, webHits, livePapers)
                val j1 = async { streamOneModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", liteModel, enrichedPrompt, 22_000L, true) }
                val j2 = async { streamOneModel("gemini-3.8-flash", "Gemini 3.8 Flash", flashModel, enrichedPrompt, 28_000L, true, preferOverLite = true) }
                val j3 = async { streamOneModel("gemini-4-argon", "Gemini 4 Argon (STEM)", argonModel, enrichedPrompt, 24_000L, false) }
                awaitAll(j1, j2, j3)
                searchJob.join()
            }
        }

        // If all cloud models failed/blank, deliver immediate Search + Local KB fallback instead of empty spinner!
        var finalErr: String? = null
        mutex.withLock {
            if (primaryText.isBlank()) {
                val diag = traces.values.joinToString(" | ") { "${it.modelLabel}: ${it.status}" }.ifBlank { "No cloud model response" }
                finalErr = diag
                primaryText = buildOfflineSearchFallback(userQuestion, ragHits, webHits, livePapers, diag)
                activeLabel = "KB + Search Fallback"
            }
        }

        // Emit done immediately without waiting for slow DOI checks
        emitSnapshot(done = true, error = finalErr)

        // Optional fast DOI check with 2.2s cap
        val doiMatches = Regex("""10\.\d{4,9}/[-._;()/:A-Za-z0-9]+""")
            .findAll(primaryText)
            .map { it.value.trimEnd('.', ',', ';', ')') }
            .distinct()
            .take(3)
            .toList()
        if (doiMatches.isNotEmpty()) {
            val verified = withTimeoutOrNull(2500L) {
                coroutineScope { doiMatches.map { doi -> async { ScientificApiClient.verifyDoi(doi) } }.awaitAll() }
            }.orEmpty()
            if (verified.isNotEmpty()) {
                emitSnapshot(done = true, error = finalErr, dois = verified)
            }
        }
    }
}
