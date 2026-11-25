package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.sqrt

data class PdbAtom(
    val x: Float,
    val y: Float,
    val z: Float,
    val chain: String,
    val resName: String,
    val resSeq: Int,
    val atomName: String,
    val isHetero: Boolean,
    val bFactor: Float = 0f,
    val secStruct: Char = 'C' // 'H' = Helix, 'E' = Sheet, 'C' = Loop/Coil
)

data class PdbDisulfide(
    val chain1: String,
    val resSeq1: Int,
    val chain2: String,
    val resSeq2: Int
)

data class PdbStructureData(
    val pdbId: String,
    val title: String,
    val method: String,
    val resolution: String,
    val chains: List<String>,
    val caAtoms: List<PdbAtom>,
    val heteroAtoms: List<PdbAtom>,
    val totalAtomsParsed: Int,
    val doi: String? = null,
    val citationAuthors: String? = null,
    val citationJournal: String? = null,
    val rFree: String? = null,
    val disulfides: List<PdbDisulfide> = emptyList(),
    val isAlphaFoldModel: Boolean = false,
    val meanPlddt: Float? = null
)

data class PdbSeqMatch(
    val pdbId: String,
    val score: Double
)

data class UniProtPdbRef(
    val pdbId: String,
    val method: String,
    val resolution: String,
    val chains: String
)

data class UniProtFeature(
    val type: String,
    val location: String,
    val description: String
)

data class UniProtSummary(
    val accession: String,
    val id: String,
    val proteinName: String,
    val geneName: String,
    val organism: String,
    val length: Int
)

data class UniProtEntry(
    val accession: String,
    val entryName: String,
    val proteinName: String,
    val geneName: String,
    val organism: String,
    val sequence: String,
    val length: Int,
    val massDa: Double,
    val functionNote: String,
    val subcellularLocations: List<String>,
    val pdbRefs: List<UniProtPdbRef>,
    val features: List<UniProtFeature>,
    val reviewed: Boolean
)

data class AlphaFoldSummary(
    val uniprotId: String,
    val entryId: String,
    val gene: String,
    val organism: String,
    val modelCreatedDate: String,
    val pdbUrl: String,
    val globalPlddt: Float? = null
)

data class LiteraturePaper(
    val id: String,
    val source: String,
    val title: String,
    val authors: String,
    val journal: String,
    val year: String,
    val doi: String?,
    val url: String?,
    val abstractText: String,
    val pubType: String
)

data class NcbiGeneHit(
    val geneId: String,
    val symbol: String,
    val name: String,
    val organism: String,
    val chromosome: String,
    val summary: String
)

data class ApiHealthStatus(
    val name: String,
    val endpoint: String,
    val ok: Boolean,
    val code: Int,
    val latencyMs: Long,
    val note: String
)

data class DoiVerification(
    val doi: String,
    val verified: Boolean,
    val title: String? = null
)

data class WebSearchHit(
    val engine: String,
    val title: String,
    val snippet: String,
    val url: String
)

data class FastSearchBundle(
    val webHits: List<WebSearchHit>,
    val papers: List<LiteraturePaper>,
    val googleSearchUrl: String,
    val elapsedMs: Long
)

object ScientificApiClient {
    private const val TAG = "ScientificApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "ImmunoVerse-Android/2.0 (Educational Biomedical Platform)")
                .build()
            chain.proceed(req)
        }
        .build()

    private val fastClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(4500, TimeUnit.MILLISECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "ImmunoVerse-Android/2.0 (Educational Biomedical Platform)")
                .build()
            chain.proceed(req)
        }
        .build()

    private suspend fun getString(url: String, useFast: Boolean = false): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().build()
        (if (useFast) fastClient else client).newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} from ${resp.request.url.host}")
            }
            resp.body?.string() ?: throw IllegalStateException("Empty response body")
        }
    }

    private fun enc(q: String): String = URLEncoder.encode(q.trim(), "UTF-8")

    private suspend fun postJson(url: String, jsonBody: String, useFast: Boolean = false): String = withContext(Dispatchers.IO) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)
        val req = Request.Builder().url(url).post(body).build()
        (if (useFast) fastClient else client).newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} from ${resp.request.url.host}")
            }
            resp.body?.string() ?: throw IllegalStateException("Empty response body")
        }
    }

    internal fun parsePdbText(
        id: String,
        pdbText: String,
        title: String,
        method: String,
        resolution: String,
        doi: String? = null,
        citationAuthors: String? = null,
        citationJournal: String? = null,
        rFree: String? = null,
        isAlphaFold: Boolean = false
    ): PdbStructureData {
        val helixRanges = mutableMapOf<String, MutableList<IntRange>>()
        val sheetRanges = mutableMapOf<String, MutableList<IntRange>>()
        val disulfides = mutableListOf<PdbDisulfide>()
        val caRaw = mutableListOf<PdbAtom>()
        val hetRaw = mutableListOf<PdbAtom>()
        val chains = linkedSetOf<String>()
        var parsedTitle = title
        var parsedMethod = method

        for (line in pdbText.lineSequence()) {
            if (line.length < 6) continue
            val rec = line.substring(0, 6).trim()
            when (rec) {
                "EXPDTA" -> if (parsedMethod == "Experimental" && line.length > 10) {
                    parsedMethod = line.substring(10).trim().ifBlank { parsedMethod }
                }
                "TITLE" -> if (parsedTitle.startsWith("RCSB PDB") && line.length > 10) {
                    val tPart = line.substring(10).trim()
                    if (tPart.isNotEmpty()) parsedTitle = tPart
                }
                "HELIX" -> if (line.length >= 38) {
                    val ch = line.substring(19, 20).trim().ifEmpty { "A" }
                    val s = line.substring(21, 25).trim().toIntOrNull()
                    val e = line.substring(33, 37).trim().toIntOrNull()
                    if (s != null && e != null && e >= s) {
                        helixRanges.getOrPut(ch) { mutableListOf() }.add(s..e)
                    }
                }
                "SHEET" -> if (line.length >= 38) {
                    val ch = line.substring(21, 22).trim().ifEmpty { "A" }
                    val s = line.substring(22, 26).trim().toIntOrNull()
                    val e = line.substring(33, 37).trim().toIntOrNull()
                    if (s != null && e != null && e >= s) {
                        sheetRanges.getOrPut(ch) { mutableListOf() }.add(s..e)
                    }
                }
                "SSBOND" -> if (line.length >= 36) {
                    val c1 = line.substring(15, 16).trim().ifEmpty { "A" }
                    val r1 = line.substring(17, 21).trim().toIntOrNull()
                    val c2 = line.substring(29, 30).trim().ifEmpty { "A" }
                    val r2 = line.substring(31, 35).trim().toIntOrNull()
                    if (r1 != null && r2 != null) {
                        disulfides += PdbDisulfide(c1, r1, c2, r2)
                    }
                }
                "ATOM" -> if (line.length >= 54) {
                    val atomName = line.substring(12, 16).trim()
                    if (atomName == "CA" || atomName == "P" || atomName == "C4'") {
                        val altLoc = line.substring(16, 17).trim()
                        if (altLoc.isNotEmpty() && altLoc != "A") continue
                        val resName = line.substring(17, 20).trim()
                        val chain = line.substring(21, 22).trim().ifEmpty { "A" }
                        val resSeq = line.substring(22, 26).trim().toIntOrNull() ?: 0
                        val x = line.substring(30, 38).trim().toFloatOrNull() ?: continue
                        val y = line.substring(38, 46).trim().toFloatOrNull() ?: continue
                        val z = line.substring(46, 54).trim().toFloatOrNull() ?: continue
                        val bf = if (line.length >= 66) line.substring(60, 66).trim().toFloatOrNull() ?: 0f else 0f
                        val ss = when {
                            helixRanges[chain]?.any { resSeq in it } == true -> 'H'
                            sheetRanges[chain]?.any { resSeq in it } == true -> 'E'
                            else -> 'C'
                        }
                        chains += chain
                        caRaw += PdbAtom(x, y, z, chain, resName, resSeq, atomName, false, bf, ss)
                    }
                }
                "HETATM" -> if (line.length >= 54 && hetRaw.size < 160) {
                    val resName = line.substring(17, 20).trim()
                    if (resName != "HOH" && resName != "DOD") {
                        val atomName = line.substring(12, 16).trim()
                        val chain = line.substring(21, 22).trim().ifEmpty { "L" }
                        val resSeq = line.substring(22, 26).trim().toIntOrNull() ?: 0
                        val x = line.substring(30, 38).trim().toFloatOrNull() ?: continue
                        val y = line.substring(38, 46).trim().toFloatOrNull() ?: continue
                        val z = line.substring(46, 54).trim().toFloatOrNull() ?: continue
                        val bf = if (line.length >= 66) line.substring(60, 66).trim().toFloatOrNull() ?: 0f else 0f
                        hetRaw += PdbAtom(x, y, z, chain, resName, resSeq, atomName, true, bf, 'C')
                    }
                }
            }
            if (caRaw.size >= 1800) break
        }

        val all = caRaw + hetRaw
        if (all.isEmpty()) throw IllegalStateException("No C-α or nucleic backbone coordinates found in $id.")
        val cx = all.map { it.x }.average().toFloat()
        val cy = all.map { it.y }.average().toFloat()
        val cz = all.map { it.z }.average().toFloat()
        var maxR = 1f
        for (a in all) {
            val d = sqrt((a.x - cx) * (a.x - cx) + (a.y - cy) * (a.y - cy) + (a.z - cz) * (a.z - cz))
            maxR = max(maxR, d)
        }
        val scale = 0.85f / maxR

        fun norm(list: List<PdbAtom>, stride: Int) = list.filterIndexed { idx, _ -> idx % stride == 0 }.map {
            it.copy(x = (it.x - cx) * scale, y = (it.y - cy) * scale, z = (it.z - cz) * scale)
        }
        val stride = if (caRaw.size > 520) (caRaw.size / 440).coerceAtLeast(1) else 1
        val meanPlddt = if (isAlphaFold && caRaw.isNotEmpty()) caRaw.map { it.bFactor }.average().toFloat() else null

        return PdbStructureData(
            pdbId = id,
            title = parsedTitle,
            method = parsedMethod,
            resolution = resolution,
            chains = chains.toList(),
            caAtoms = norm(caRaw, stride),
            heteroAtoms = norm(hetRaw, 1),
            totalAtomsParsed = caRaw.size + hetRaw.size,
            doi = doi,
            citationAuthors = citationAuthors,
            citationJournal = citationJournal,
            rFree = rFree,
            disulfides = disulfides,
            isAlphaFoldModel = isAlphaFold,
            meanPlddt = meanPlddt
        )
    }

    suspend fun fetchRcsbPdb(rawId: String): Result<PdbStructureData> = runCatching {
        val pdbId = rawId.trim().uppercase().take(8)
        require(pdbId.length == 4) { "Enter a valid 4-character PDB ID (e.g. 1UBQ, 1IGT, 1AO7)." }

        val metaJson = try {
            JSONObject(getString("https://data.rcsb.org/rest/v1/core/entry/$pdbId"))
        } catch (_: Exception) {
            null
        }
        val title = metaJson?.optJSONObject("struct")?.optString("title").orEmpty().ifBlank { "RCSB PDB $pdbId" }
        val method = metaJson?.optJSONArray("exptl")?.optJSONObject(0)?.optString("method").orEmpty().ifBlank { "Experimental" }
        val resArr = metaJson?.optJSONObject("rcsb_entry_info")?.optJSONArray("resolution_combined")
        val resolution = if (resArr != null && resArr.length() > 0) "${"%.2f".format(resArr.optDouble(0))} Å" else "N/A"
        val rFreeVal = metaJson?.optJSONArray("refine")?.optJSONObject(0)?.optDouble("ls_R_factor_R_free", Double.NaN)
        val rFree = if (rFreeVal != null && !rFreeVal.isNaN()) "${"%.3f".format(rFreeVal)}" else null
        val citObj = metaJson?.optJSONObject("rcsb_primary_citation")
        val doi = citObj?.optString("pdbx_database_id_DOI")?.takeIf { it.isNotBlank() }
        val citJournal = citObj?.let {
            val j = it.optString("journal_abbrev")
            val y = it.optInt("year", 0)
            if (j.isNotBlank() && y > 0) "$j ($y)" else j.takeIf { s -> s.isNotBlank() }
        }
        val authorsArr = citObj?.optJSONArray("rcsb_authors")
        val citAuthors = if (authorsArr != null && authorsArr.length() > 0) {
            val first = authorsArr.optString(0)
            if (authorsArr.length() > 1) "$first et al." else first
        } else null

        val pdbText = getString("https://files.rcsb.org/download/$pdbId.pdb")
        parsePdbText(
            id = pdbId,
            pdbText = pdbText,
            title = title,
            method = method,
            resolution = resolution,
            doi = doi,
            citationAuthors = citAuthors,
            citationJournal = citJournal,
            rFree = rFree,
            isAlphaFold = false
        )
    }

    suspend fun fetchAlphaFold(uniprotId: String): Result<AlphaFoldSummary> = runCatching {
        val id = uniprotId.trim().uppercase()
        require(id.isNotBlank()) { "UniProt ID required." }
        val arr = JSONArray(getString("https://alphafold.ebi.ac.uk/api/prediction/$id"))
        if (arr.length() == 0) throw IllegalStateException("No AlphaFold entry found for $id.")
        val obj = arr.getJSONObject(0)
        val metric = obj.optDouble("globalMetricValue", Double.NaN)
        AlphaFoldSummary(
            uniprotId = id,
            entryId = obj.optString("entryId", id),
            gene = obj.optString("gene", "N/A"),
            organism = obj.optString("organismScientificName", "Unknown"),
            modelCreatedDate = obj.optString("modelCreatedDate", ""),
            pdbUrl = obj.optString("pdbUrl", ""),
            globalPlddt = if (!metric.isNaN()) metric.toFloat() else null
        )
    }

    suspend fun fetchAlphaFoldStructure(uniprotId: String): Result<PdbStructureData> = runCatching {
        val summary = fetchAlphaFold(uniprotId).getOrThrow()
        val url = summary.pdbUrl.ifBlank {
            throw IllegalStateException("AlphaFold DB did not return a PDB coordinate URL for ${summary.uniprotId}.")
        }
        val pdbText = getString(url)
        parsePdbText(
            id = summary.entryId,
            pdbText = pdbText,
            title = "${summary.gene.ifBlank { summary.uniprotId }} (${summary.organism}) — AlphaFold DB Model",
            method = "AlphaFold2 / DeepMind Structure Prediction",
            resolution = summary.globalPlddt?.let { "pLDDT ${"%.1f".format(it)}/100" } ?: "Predicted",
            doi = "10.1038/s41586-021-03819-2",
            citationAuthors = "Jumper et al. (AlphaFold DB)",
            citationJournal = "Nature / EMBL-EBI (${summary.modelCreatedDate})",
            isAlphaFold = true
        )
    }

    suspend fun searchRcsbBySequence(aaSequence: String): Result<List<PdbSeqMatch>> = runCatching {
        val clean = aaSequence.uppercase().filter { it in "ACDEFGHIKLMNPQRSTVWY" }
        require(clean.length >= 16) { "Enter at least 16 amino acids to search RCSB PDB for experimental homologs." }
        val reqObj = JSONObject().apply {
            put("query", JSONObject().apply {
                put("type", "terminal")
                put("service", "sequence")
                put("parameters", JSONObject().apply {
                    put("evalue_cutoff", 0.1)
                    put("identity_cutoff", 0.30)
                    put("sequence_type", "protein")
                    put("value", clean.take(1200))
                })
            })
            put("request_options", JSONObject().apply {
                put("paginate", JSONObject().apply {
                    put("start", 0)
                    put("rows", 6)
                })
            })
            put("return_type", "entry")
        }
        val resp = JSONObject(postJson("https://search.rcsb.org/rcsbsearch/v2/query", reqObj.toString()))
        val arr = resp.optJSONArray("result_set") ?: JSONArray()
        (0 until arr.length()).mapNotNull { idx ->
            val item = arr.optJSONObject(idx) ?: return@mapNotNull null
            val pid = item.optString("identifier").takeIf { it.length == 4 } ?: return@mapNotNull null
            PdbSeqMatch(pdbId = pid, score = item.optDouble("score", 0.0))
        }
    }

    suspend fun searchUniProt(query: String): Result<List<UniProtSummary>> = runCatching {
        val q = query.trim()
        require(q.isNotBlank()) { "Enter a protein name, gene symbol, or UniProt accession." }
        val url = "https://rest.uniprot.org/uniprotkb/search?query=${enc("$q AND reviewed:true")}&format=json&size=8&fields=accession,id,protein_name,gene_names,organism_name,length"
        val json = JSONObject(getString(url, useFast = true))
        val results = json.optJSONArray("results") ?: JSONArray()
        (0 until results.length()).map { idx ->
            val r = results.getJSONObject(idx)
            val protName = r.optJSONObject("proteinDescription")
                ?.optJSONObject("recommendedName")
                ?.optJSONObject("fullName")
                ?.optString("value")
                .orEmpty()
                .ifBlank { r.optString("uniProtkbId", "Protein") }
            val gene = r.optJSONArray("genes")
                ?.optJSONObject(0)
                ?.optJSONObject("geneName")
                ?.optString("value")
                .orEmpty()
                .ifBlank { "—" }
            val org = r.optJSONObject("organism")?.optString("scientificName").orEmpty().ifBlank { "Unknown" }
            UniProtSummary(
                accession = r.optString("primaryAccession"),
                id = r.optString("uniProtkbId"),
                proteinName = protName,
                geneName = gene,
                organism = org,
                length = r.optJSONObject("sequence")?.optInt("length", 0) ?: 0
            )
        }
    }

    suspend fun fetchUniProtEntry(rawAccession: String): Result<UniProtEntry> = runCatching {
        val acc = rawAccession.trim().uppercase()
        require(acc.isNotBlank()) { "UniProt accession required (e.g. P01579, P01375)." }
        val r = JSONObject(getString("https://rest.uniprot.org/uniprotkb/$acc.json"))
        val protName = r.optJSONObject("proteinDescription")
            ?.optJSONObject("recommendedName")
            ?.optJSONObject("fullName")
            ?.optString("value")
            .orEmpty()
            .ifBlank { r.optString("uniProtkbId", acc) }
        val gene = r.optJSONArray("genes")
            ?.optJSONObject(0)
            ?.optJSONObject("geneName")
            ?.optString("value")
            .orEmpty()
            .ifBlank { "—" }
        val org = r.optJSONObject("organism")?.optString("scientificName").orEmpty().ifBlank { "Unknown" }
        val seqObj = r.optJSONObject("sequence")
        val seq = seqObj?.optString("value").orEmpty()
        val length = seqObj?.optInt("length", seq.length) ?: seq.length
        val massDa = seqObj?.optDouble("molWeight", 0.0) ?: 0.0

        val comments = r.optJSONArray("comments") ?: JSONArray()
        var fnText = ""
        val subLocs = mutableListOf<String>()
        for (i in 0 until comments.length()) {
            val c = comments.optJSONObject(i) ?: continue
            when (c.optString("commentType")) {
                "FUNCTION" -> if (fnText.isBlank()) {
                    fnText = c.optJSONArray("texts")?.optJSONObject(0)?.optString("value").orEmpty()
                }
                "SUBCELLULAR LOCATION" -> {
                    val locs = c.optJSONArray("subcellularLocations")
                    if (locs != null) {
                        for (k in 0 until locs.length()) {
                            val valStr = locs.optJSONObject(k)?.optJSONObject("location")?.optString("value").orEmpty()
                            if (valStr.isNotBlank() && valStr !in subLocs) subLocs += valStr
                        }
                    }
                }
            }
        }

        val xrefs = r.optJSONArray("uniProtKBCrossReferences") ?: JSONArray()
        val pdbRefs = mutableListOf<UniProtPdbRef>()
        for (i in 0 until xrefs.length()) {
            val xr = xrefs.optJSONObject(i) ?: continue
            if (xr.optString("database") == "PDB") {
                val props = xr.optJSONArray("properties") ?: JSONArray()
                var m = "Experimental"
                var res = "N/A"
                var ch = ""
                for (p in 0 until props.length()) {
                    val pr = props.optJSONObject(p) ?: continue
                    when (pr.optString("key")) {
                        "Method" -> m = pr.optString("value", m)
                        "Resolution" -> res = pr.optString("value", res)
                        "Chains" -> ch = pr.optString("value", ch)
                    }
                }
                pdbRefs += UniProtPdbRef(xr.optString("id"), m, res, ch)
            }
        }

        val featsArr = r.optJSONArray("features") ?: JSONArray()
        val features = mutableListOf<UniProtFeature>()
        val interestingTypes = setOf("Active site", "Binding site", "Modified residue", "Glycosylation", "Disulfide bond", "Domain", "Signal")
        for (i in 0 until featsArr.length()) {
            val f = featsArr.optJSONObject(i) ?: continue
            val t = f.optString("type")
            if (t in interestingTypes && features.size < 25) {
                val loc = f.optJSONObject("location")
                val s = loc?.optJSONObject("start")?.optInt("value", 0) ?: 0
                val e = loc?.optJSONObject("end")?.optInt("value", s) ?: s
                val locLabel = if (s == e) "$s" else "$s–$e"
                features += UniProtFeature(t, locLabel, f.optString("description", t))
            }
        }

        UniProtEntry(
            accession = r.optString("primaryAccession", acc),
            entryName = r.optString("uniProtkbId", acc),
            proteinName = protName,
            geneName = gene,
            organism = org,
            sequence = seq,
            length = length,
            massDa = massDa,
            functionNote = fnText.ifBlank { "Reviewed UniProtKB record." },
            subcellularLocations = subLocs.take(6),
            pdbRefs = pdbRefs.take(12),
            features = features,
            reviewed = r.optString("entryType").contains("reviewed", ignoreCase = true)
        )
    }

    suspend fun searchEuropePmc(query: String, preprintsOnly: Boolean = false, yearFrom: Int? = null): Result<List<LiteraturePaper>> = runCatching {
        val parts = mutableListOf(query.trim())
        if (preprintsOnly) parts += "SRC:PPR"
        if (yearFrom != null) parts += "PUB_YEAR:[$yearFrom TO 2030]"
        val fullQuery = parts.joinToString(" AND ")
        val url = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=${enc(fullQuery)}&format=json&resultType=core&pageSize=12"
        val json = JSONObject(getString(url))
        val results = json.optJSONObject("resultList")?.optJSONArray("result") ?: JSONArray()
        (0 until results.length()).map { idx ->
            val r = results.getJSONObject(idx)
            val doi = r.optString("doi").takeIf { it.isNotBlank() }
            LiteraturePaper(
                id = "epmc_${r.optString("id", idx.toString())}",
                source = if (r.optString("source") == "PPR") "bioRxiv/medRxiv (Europe PMC)" else "Europe PMC",
                title = r.optString("title", "Untitled").replace(Regex("<[^>]+>"), ""),
                authors = r.optString("authorString", "Authors unavailable"),
                journal = r.optString("journalTitle", r.optString("bookOrReportDetails", "Preprint / Journal")),
                year = r.optString("pubYear", ""),
                doi = doi,
                url = doi?.let { "https://doi.org/$it" } ?: "https://europepmc.org/article/${r.optString("source", "MED")}/${r.optString("id")}",
                abstractText = r.optString("abstractText", "No abstract provided in API metadata.").replace(Regex("<[^>]+>"), ""),
                pubType = r.optJSONObject("pubTypeList")?.optJSONArray("pubType")?.optString(0).orEmpty().ifBlank { "Article" }
            )
        }
    }

    suspend fun searchPubMed(query: String): Result<List<LiteraturePaper>> = runCatching {
        val searchUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=${enc(query)}&retmode=json&retmax=10"
        val sJson = JSONObject(getString(searchUrl))
        val idList = sJson.optJSONObject("esearchresult")?.optJSONArray("idlist") ?: JSONArray()
        if (idList.length() == 0) return@runCatching emptyList()
        val ids = (0 until idList.length()).joinToString(",") { idList.getString(it) }
        val sumUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=$ids&retmode=json"
        val sumJson = JSONObject(getString(sumUrl)).optJSONObject("result") ?: JSONObject()
        val papers = mutableListOf<LiteraturePaper>()
        for (i in 0 until idList.length()) {
            val pmid = idList.getString(i)
            val item = sumJson.optJSONObject(pmid) ?: continue
            val authorsArr = item.optJSONArray("authors")
            val auth = if (authorsArr != null) {
                (0 until minOf(4, authorsArr.length())).joinToString(", ") { authorsArr.getJSONObject(it).optString("name") }
            } else "Authors unavailable"
            val articleIds = item.optJSONArray("articleids")
            var doi: String? = null
            if (articleIds != null) {
                for (k in 0 until articleIds.length()) {
                    val aid = articleIds.getJSONObject(k)
                    if (aid.optString("idtype") == "doi") doi = aid.optString("value")
                }
            }
            papers += LiteraturePaper(
                id = "pmid_$pmid",
                source = "PubMed",
                title = item.optString("title", "Untitled"),
                authors = auth,
                journal = item.optString("fulljournalname", item.optString("source", "PubMed")),
                year = item.optString("pubdate", "").take(4),
                doi = doi,
                url = "https://pubmed.ncbi.nlm.nih.gov/$pmid/",
                abstractText = "Open via PubMed PMID $pmid for full structured abstract and MeSH terms.",
                pubType = item.optJSONArray("pubtype")?.optString(0).orEmpty().ifBlank { "Journal Article" }
            )
        }
        papers
    }

    suspend fun searchCrossref(query: String): Result<List<LiteraturePaper>> = runCatching {
        val url = "https://api.crossref.org/works?query=${enc(query)}&rows=10"
        val json = JSONObject(getString(url))
        val items = json.optJSONObject("message")?.optJSONArray("items") ?: JSONArray()
        (0 until items.length()).map { idx ->
            val item = items.getJSONObject(idx)
            val title = item.optJSONArray("title")?.optString(0).orEmpty().ifBlank { "Untitled" }
            val authArr = item.optJSONArray("author")
            val authors = if (authArr != null) {
                (0 until minOf(4, authArr.length())).joinToString(", ") {
                    val a = authArr.getJSONObject(it)
                    "${a.optString("family")} ${a.optString("given").take(1)}".trim()
                }
            } else "Authors unavailable"
            val doi = item.optString("DOI").takeIf { it.isNotBlank() }
            val dateParts = item.optJSONObject("issued")?.optJSONArray("date-parts")?.optJSONArray(0)
            val year = dateParts?.optInt(0)?.toString().orEmpty()
            LiteraturePaper(
                id = "cr_${doi ?: idx}",
                source = "Crossref",
                title = title,
                authors = authors,
                journal = item.optJSONArray("container-title")?.optString(0).orEmpty().ifBlank { "Crossref Work" },
                year = year,
                doi = doi,
                url = doi?.let { "https://doi.org/$it" },
                abstractText = item.optString("abstract", "Abstract not deposited in Crossref metadata.").replace(Regex("<[^>]+>"), ""),
                pubType = item.optString("type", "journal-article")
            )
        }
    }

    suspend fun searchOpenAlex(query: String): Result<List<LiteraturePaper>> = runCatching {
        val url = "https://api.openalex.org/works?search=${enc(query)}&per-page=10"
        val json = JSONObject(getString(url))
        val results = json.optJSONArray("results") ?: JSONArray()
        (0 until results.length()).map { idx ->
            val w = results.getJSONObject(idx)
            val auths = w.optJSONArray("authorships")
            val authors = if (auths != null) {
                (0 until minOf(4, auths.length())).joinToString(", ") {
                    auths.getJSONObject(it).optJSONObject("author")?.optString("display_name").orEmpty()
                }
            } else "Authors unavailable"
            val rawDoi = w.optString("doi").removePrefix("https://doi.org/").takeIf { it.isNotBlank() }
            LiteraturePaper(
                id = "oa_${w.optString("id", idx.toString()).substringAfterLast("/")}",
                source = "OpenAlex",
                title = w.optString("title", "Untitled"),
                authors = authors,
                journal = w.optJSONObject("primary_location")?.optJSONObject("source")?.optString("display_name").orEmpty().ifBlank { "OpenAlex" },
                year = w.optInt("publication_year", 0).let { if (it > 0) it.toString() else "" },
                doi = rawDoi,
                url = w.optString("doi").takeIf { it.isNotBlank() } ?: w.optString("id"),
                abstractText = "OpenAlex work (${w.optInt("cited_by_count", 0)} citations).",
                pubType = w.optString("type", "article")
            )
        }
    }

    suspend fun searchSemanticScholar(query: String): Result<List<LiteraturePaper>> = runCatching {
        val url = "https://api.semanticscholar.org/graph/v1/paper/search?query=${enc(query)}&limit=8&fields=title,authors,year,venue,externalIds,url,abstract"
        val json = JSONObject(getString(url))
        val data = json.optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { idx ->
            val p = data.getJSONObject(idx)
            val authArr = p.optJSONArray("authors")
            val authors = if (authArr != null) {
                (0 until minOf(4, authArr.length())).joinToString(", ") { authArr.getJSONObject(it).optString("name") }
            } else "Authors unavailable"
            val doi = p.optJSONObject("externalIds")?.optString("DOI")?.takeIf { it.isNotBlank() }
            LiteraturePaper(
                id = "s2_${p.optString("paperId", idx.toString())}",
                source = "Semantic Scholar",
                title = p.optString("title", "Untitled"),
                authors = authors,
                journal = p.optString("venue", "Semantic Scholar"),
                year = p.optInt("year", 0).let { if (it > 0) it.toString() else "" },
                doi = doi,
                url = p.optString("url").takeIf { it.isNotBlank() },
                abstractText = p.optString("abstract", "No abstract indexed."),
                pubType = "Article"
            )
        }
    }

    suspend fun searchNcbiGene(symbolOrTerm: String): Result<List<NcbiGeneHit>> = runCatching {
        val esUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gene&term=${enc("$symbolOrTerm[Gene Name] AND Homo sapiens[Organism]")}&retmode=json&retmax=6"
        val esJson = JSONObject(getString(esUrl))
        val ids = esJson.optJSONObject("esearchresult")?.optJSONArray("idlist") ?: JSONArray()
        if (ids.length() == 0) return@runCatching emptyList()
        val joined = (0 until ids.length()).joinToString(",") { ids.getString(it) }
        val sumUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gene&id=$joined&retmode=json"
        val res = JSONObject(getString(sumUrl)).optJSONObject("result") ?: JSONObject()
        val hits = mutableListOf<NcbiGeneHit>()
        for (i in 0 until ids.length()) {
            val gid = ids.getString(i)
            val g = res.optJSONObject(gid) ?: continue
            hits += NcbiGeneHit(
                geneId = gid,
                symbol = g.optString("name", gid),
                name = g.optString("description", ""),
                organism = g.optJSONObject("organism")?.optString("scientificname", "Homo sapiens") ?: "Homo sapiens",
                chromosome = g.optString("chromosome", "?"),
                summary = g.optString("summary", "No NCBI Gene summary text.").ifBlank { "No NCBI Gene summary text." }
            )
        }
        hits
    }

    suspend fun verifyDoi(doi: String): DoiVerification = withContext(Dispatchers.IO) {
        val clean = doi.trim().removePrefix("https://doi.org/")
        try {
            val url = "https://api.crossref.org/works/${enc(clean)}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val title = JSONObject(body).optJSONObject("message")?.optJSONArray("title")?.optString(0)
                    DoiVerification(clean, true, title)
                } else {
                    DoiVerification(clean, false, null)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DOI check error for $clean: ${e.message}")
            DoiVerification(clean, false, null)
        }
    }

    suspend fun searchWikipediaScience(query: String): Result<List<WebSearchHit>> = runCatching {
        val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${enc(query)}&format=json&srlimit=3"
        val json = JSONObject(getString(url, useFast = true))
        val arr = json.optJSONObject("query")?.optJSONArray("search") ?: JSONArray()
        (0 until arr.length()).map { idx ->
            val item = arr.getJSONObject(idx)
            val title = item.optString("title", "Wikipedia")
            val snippet = item.optString("snippet", "").replace(Regex("<[^>]+>"), "")
            WebSearchHit(
                engine = "Wikipedia Search",
                title = title,
                snippet = snippet,
                url = "https://en.wikipedia.org/wiki/${enc(title.replace(' ', '_'))}"
            )
        }
    }

    suspend fun searchDuckDuckGoInstant(query: String): Result<List<WebSearchHit>> = runCatching {
        val url = "https://api.duckduckgo.com/?q=${enc(query)}&format=json&no_html=1&skip_disambig=1"
        val json = JSONObject(getString(url, useFast = true))
        val hits = mutableListOf<WebSearchHit>()
        val abs = json.optString("AbstractText").trim()
        if (abs.isNotEmpty()) {
            hits += WebSearchHit(
                engine = "Web Knowledge Graph",
                title = json.optString("Heading", query).ifBlank { query },
                snippet = abs,
                url = json.optString("AbstractURL", "https://www.google.com/search?q=${enc(query)}")
            )
        }
        val related = json.optJSONArray("RelatedTopics")
        if (related != null) {
            for (i in 0 until minOf(2, related.length())) {
                val obj = related.optJSONObject(i) ?: continue
                val txt = obj.optString("Text").trim()
                val firstUrl = obj.optString("FirstURL").trim()
                if (txt.isNotEmpty() && firstUrl.isNotEmpty()) {
                    hits += WebSearchHit("Web Search", txt.take(64), txt, firstUrl)
                }
            }
        }
        hits
    }

    suspend fun fastSearchWebAndLiterature(query: String): FastSearchBundle = coroutineScope {
        val start = System.currentTimeMillis()
        val wikiJob = async { withTimeoutOrNull(3200L) { searchWikipediaScience(query).getOrDefault(emptyList()) } ?: emptyList() }
        val ddgJob = async { withTimeoutOrNull(3000L) { searchDuckDuckGoInstant(query).getOrDefault(emptyList()) } ?: emptyList() }
        val epmcJob = async {
            withTimeoutOrNull(3400L) {
                runCatching {
                    val url = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=${enc(query.trim())}&format=json&resultType=lite&pageSize=4"
                    val json = JSONObject(getString(url, useFast = true))
                    val results = json.optJSONObject("resultList")?.optJSONArray("result") ?: JSONArray()
                    (0 until results.length()).map { idx ->
                        val r = results.getJSONObject(idx)
                        val doi = r.optString("doi").takeIf { it.isNotBlank() }
                        LiteraturePaper(
                            id = "epmc_fast_${r.optString("id", idx.toString())}",
                            source = "Europe PMC",
                            title = r.optString("title", "Untitled").replace(Regex("<[^>]+>"), ""),
                            authors = r.optString("authorString", "Authors unavailable"),
                            journal = r.optString("journalTitle", "Journal"),
                            year = r.optString("pubYear", ""),
                            doi = doi,
                            url = doi?.let { "https://doi.org/$it" } ?: "https://europepmc.org/article/${r.optString("source", "MED")}/${r.optString("id")}",
                            abstractText = "",
                            pubType = "Article"
                        )
                    }
                }.getOrDefault(emptyList())
            } ?: emptyList()
        }
        val oaJob = async {
            withTimeoutOrNull(3200L) {
                runCatching {
                    val url = "https://api.openalex.org/works?search=${enc(query)}&per-page=3"
                    val json = JSONObject(getString(url, useFast = true))
                    val results = json.optJSONArray("results") ?: JSONArray()
                    (0 until results.length()).map { idx ->
                        val w = results.getJSONObject(idx)
                        val rawDoi = w.optString("doi").removePrefix("https://doi.org/").takeIf { it.isNotBlank() }
                        LiteraturePaper(
                            id = "oa_fast_$idx",
                            source = "OpenAlex",
                            title = w.optString("title", "Untitled"),
                            authors = "",
                            journal = w.optJSONObject("primary_location")?.optJSONObject("source")?.optString("display_name").orEmpty().ifBlank { "OpenAlex" },
                            year = w.optInt("publication_year", 0).let { if (it > 0) it.toString() else "" },
                            doi = rawDoi,
                            url = w.optString("doi").takeIf { it.isNotBlank() } ?: w.optString("id"),
                            abstractText = "",
                            pubType = "Article"
                        )
                    }
                }.getOrDefault(emptyList())
            } ?: emptyList()
        }

        val webHits = (wikiJob.await() + ddgJob.await()).distinctBy { it.title }.take(5)
        val papers = (epmcJob.await() + oaJob.await()).distinctBy { it.title.lowercase() }.take(5)
        FastSearchBundle(
            webHits = webHits,
            papers = papers,
            googleSearchUrl = "https://www.google.com/search?q=${enc(query)}",
            elapsedMs = System.currentTimeMillis() - start
        )
    }

    suspend fun checkAllApis(): List<ApiHealthStatus> = withContext(Dispatchers.IO) {
        val probes = listOf(
            Triple("Europe PMC", "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=immunology&format=json&pageSize=1", "Literature & Preprints"),
            Triple("NCBI PubMed", "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=cytokine&retmode=json&retmax=1", "E-utilities & Gene"),
            Triple("Crossref", "https://api.crossref.org/works?rows=1", "DOI Verification"),
            Triple("OpenAlex", "https://api.openalex.org/works?per-page=1", "Open Scholarly Graph"),
            Triple("RCSB PDB", "https://data.rcsb.org/rest/v1/core/entry/1IGT", "3D Molecular Coordinates"),
            Triple("AlphaFold DB", "https://alphafold.ebi.ac.uk/api/prediction/P01579", "Predicted Structures")
        )
        probes.map { (name, url, note) ->
            val start = System.currentTimeMillis()
            try {
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val ms = System.currentTimeMillis() - start
                    ApiHealthStatus(name, url, resp.isSuccessful, resp.code, ms, note)
                }
            } catch (e: Exception) {
                val ms = System.currentTimeMillis() - start
                ApiHealthStatus(name, url, false, 0, ms, e.localizedMessage ?: "Network unreachable")
            }
        }
    }
}
