package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.network.DoiVerification
import com.example.data.network.LiteraturePaper
import com.example.data.network.WebSearchHit
import com.example.data.repo.AiExecutionMode
import com.example.data.repo.ImmunoAiRepository
import com.example.data.repo.ModelAnswerTrace
import com.example.data.repo.RagChunk
import com.example.data.repo.UploadedDoc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatTurn(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val activeModelLabel: String = "",
    val ragChunks: List<RagChunk> = emptyList(),
    val webHits: List<WebSearchHit> = emptyList(),
    val livePapers: List<LiteraturePaper> = emptyList(),
    val verifiedDois: List<DoiVerification> = emptyList(),
    val modelTraces: List<ModelAnswerTrace> = emptyList(),
    val googleSearchUrl: String? = null,
    val error: String? = null
)

class ImmunoAiViewModel : ViewModel() {
    private val repo = ImmunoAiRepository()

    private val _messages = MutableStateFlow<List<ChatTurn>>(
        listOf(
            ChatTurn(
                id = "welcome",
                isUser = false,
                activeModelLabel = "Multi-Model + Search",
                text = "Welcome to Immuno AI. Ask any question across immunology, virology, genomics, or your uploaded files. Turbo Multi-Model streams Gemini 3.5 Flash-Lite immediately while pulling live Web/Wikipedia/Europe PMC/OpenAlex search results & Gemini 3.8 Flash synthesis in parallel."
            )
        )
    )
    val messages = _messages.asStateFlow()

    private val _uploadedDocs = MutableStateFlow<List<UploadedDoc>>(emptyList())
    val uploadedDocs = _uploadedDocs.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _useLiveCitations = MutableStateFlow(true)
    val useLiveCitations = _useLiveCitations.asStateFlow()

    private val _executionMode = MutableStateFlow(AiExecutionMode.TURBO_RACE)
    val executionMode = _executionMode.asStateFlow()

    private var activeJob: Job? = null

    fun setLiveCitations(enabled: Boolean) {
        _useLiveCitations.value = enabled
    }

    fun setExecutionMode(mode: AiExecutionMode) {
        _executionMode.value = mode
    }

    fun addUploadedDocument(name: String, content: String) {
        val doc = UploadedDoc(
            id = "doc_${System.currentTimeMillis()}",
            name = name,
            content = content.take(60_000)
        )
        _uploadedDocs.value = _uploadedDocs.value + doc
    }

    fun removeUploadedDocument(id: String) {
        _uploadedDocs.value = _uploadedDocs.value.filterNot { it.id == id }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        activeJob = null
        _isGenerating.value = false
    }

    fun clearChat() {
        stopGeneration()
        _messages.value = _messages.value.take(1)
    }

    fun sendQuestion(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        if (_isGenerating.value) {
            stopGeneration()
        }

        val userTurn = ChatTurn("u_${System.currentTimeMillis()}", isUser = true, text = clean)
        val assistantId = "a_${System.currentTimeMillis()}"
        val placeholder = ChatTurn(assistantId, isUser = false, text = "", activeModelLabel = _executionMode.value.label)
        _messages.value = _messages.value + userTurn + placeholder
        _isGenerating.value = true

        activeJob = viewModelScope.launch {
            try {
                repo.askStream(
                    userQuestion = clean,
                    uploadedDocs = _uploadedDocs.value,
                    fetchLiveCitations = _useLiveCitations.value,
                    mode = _executionMode.value
                ).collect { ev ->
                    _messages.value = _messages.value.map { turn ->
                        if (turn.id == assistantId) {
                            turn.copy(
                                text = ev.partialText,
                                activeModelLabel = ev.activeModelLabel,
                                ragChunks = ev.ragChunks,
                                webHits = ev.webHits,
                                livePapers = ev.livePapers,
                                verifiedDois = ev.verifiedDois,
                                modelTraces = ev.modelTraces,
                                googleSearchUrl = ev.googleSearchUrl,
                                error = ev.error
                            )
                        } else turn
                    }
                    if (ev.done) {
                        _isGenerating.value = false
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.value = _messages.value.map { turn ->
                    if (turn.id == assistantId && turn.text.isBlank()) {
                        turn.copy(text = "Error: ${e.localizedMessage ?: "AI request failed"}", error = e.localizedMessage)
                    } else turn
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
