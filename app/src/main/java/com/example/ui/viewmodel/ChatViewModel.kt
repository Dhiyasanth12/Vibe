package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.data.repository.VibeAiRepository
import com.example.data.repository.VibeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: VibeRepository,
    private val aiRepository: VibeAiRepository,
    private val currentUserId: String,
    private val conversationId: String
) : ViewModel() {

    val messages: StateFlow<UiState<List<ChatMessage>>> =
        repository.observeMessages(currentUserId, conversationId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.message ?: "Failed to load messages"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _smartRepliesState = MutableStateFlow<UiState<List<String>>>(UiState.Success(emptyList()))
    val smartRepliesState: StateFlow<UiState<List<String>>> = _smartRepliesState.asStateFlow()

    private val _aiAssistantState = MutableStateFlow<UiState<String>>(UiState.Success(""))
    val aiAssistantState: StateFlow<UiState<String>> = _aiAssistantState.asStateFlow()

    private val _rewriteState = MutableStateFlow<UiState<String>>(UiState.Success(""))
    val rewriteState: StateFlow<UiState<String>> = _rewriteState.asStateFlow()

    fun sendMessage(
        conversation: Conversation,
        senderProfile: VibeProfile,
        text: String,
        mediaType: String = "text",
        mediaData: String = "",
        voiceDurationSec: Long = 0L,
        replyTo: ChatMessage? = null,
        forwardedFrom: String = ""
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                conversation = conversation,
                senderProfile = senderProfile,
                text = text,
                mediaType = mediaType,
                mediaData = mediaData,
                voiceDurationSec = voiceDurationSec,
                replyTo = replyTo,
                forwardedFrom = forwardedFrom
            )
        }
    }

    fun updateTyping(conversation: Conversation, profileId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.updateTypingStatus(conversation, profileId, isTyping)
        }
    }

    fun markRead(messages: List<ChatMessage>, readerProfileId: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(conversationId, messages, readerProfileId)
        }
    }

    fun toggleReaction(message: ChatMessage, profileId: String, emoji: String) {
        viewModelScope.launch {
            repository.toggleMessageReaction(conversationId, message, profileId, emoji)
        }
    }

    fun updateChatTheme(themeId: String) {
        viewModelScope.launch {
            repository.updateChatTheme(conversationId, themeId)
        }
    }

    fun updateDisappearingTimer(seconds: Long) {
        viewModelScope.launch {
            repository.updateDisappearingTimer(conversationId, seconds)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(conversationId, messageId)
        }
    }

    fun requestSmartReplies(activeMood: String) {
        val currentMsgs = (messages.value as? UiState.Success)?.data.orEmpty()
        val snippets = currentMsgs.takeLast(6).map { "${it.senderName}: ${it.text}" }
        viewModelScope.launch {
            _smartRepliesState.value = UiState.Loading
            aiRepository.suggestReplies(snippets.ifEmpty { listOf("Starting a new chat on VIBE") }, activeMood)
                .onSuccess { _smartRepliesState.value = UiState.Success(it) }
                .onFailure { _smartRepliesState.value = UiState.Error(it.message ?: "VibeAI error") }
        }
    }

    fun rewriteDraft(draft: String, tone: String) {
        if (draft.isBlank()) return
        viewModelScope.launch {
            _rewriteState.value = UiState.Loading
            aiRepository.rewriteMessageTone(draft, tone)
                .onSuccess { _rewriteState.value = UiState.Success(it) }
                .onFailure { _rewriteState.value = UiState.Error(it.message ?: "Rewrite failed") }
        }
    }

    fun askVibeAi(prompt: String) {
        viewModelScope.launch {
            _aiAssistantState.value = UiState.Loading
            try {
                aiRepository.streamAssistant(prompt).collect { partial ->
                    _aiAssistantState.value = UiState.Success(partial)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiAssistantState.value = UiState.Error(e.message ?: "VibeAI failed")
            }
        }
    }
}
