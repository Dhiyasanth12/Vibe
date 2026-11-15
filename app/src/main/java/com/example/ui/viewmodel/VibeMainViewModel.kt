package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioRoom
import com.example.data.model.Conversation
import com.example.data.model.FollowRelation
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostLike
import com.example.data.model.ProfileCredential
import com.example.data.model.ProfileNotification
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.VibeProfile
import com.example.data.repository.ActiveProfileStore
import com.example.data.repository.VibeAiRepository
import com.example.data.repository.VibeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VibeMainViewModel(
    private val repository: VibeRepository,
    private val profileStore: ActiveProfileStore,
    private val aiRepository: VibeAiRepository,
    val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _aiCaptionState = MutableStateFlow<UiState<String>>(UiState.Success(""))
    val aiCaptionState: StateFlow<UiState<String>> = _aiCaptionState.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = profileStore.observeDarkTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private fun <T> Flow<T>.toUiStateFlow(initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.message ?: "Failed to load data"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val userAccount: StateFlow<UiState<UserAccount?>> =
        repository.observeUserAccount(currentUserId).toUiStateFlow()

    val myProfiles: StateFlow<UiState<List<VibeProfile>>> =
        repository.observeMyProfiles(currentUserId).toUiStateFlow()

    val myCredentials: StateFlow<List<ProfileCredential>> =
        repository.observeMyProfileCredentials(currentUserId)
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val loggedOutProfileIds: StateFlow<Set<String>> = combine(
        myCredentials,
        profileStore.observeLocallyLoggedOutProfileIds(currentUserId)
    ) { creds, localSet ->
        val remoteLoggedOut = creds.filter { it.isLoggedOut }.map { it.profileId }.toSet()
        remoteLoggedOut + localSet
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val storedActiveProfileId: StateFlow<String?> =
        profileStore.observeActiveProfileId(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeProfile: StateFlow<VibeProfile?> = combine(
        myProfiles,
        userAccount,
        storedActiveProfileId,
        loggedOutProfileIds
    ) { profilesState, accountState, localPrefId, loggedOutIds ->
        val list = (profilesState as? UiState.Success)?.data.orEmpty()
        if (list.isEmpty()) return@combine null
        val loggedInList = list.filter { it.id !in loggedOutIds }
        if (loggedInList.isEmpty()) return@combine null
        val activePool = loggedInList.filter { !it.isDeactivated }
        val pool = activePool.ifEmpty { loggedInList }
        val accountActiveId = (accountState as? UiState.Success)?.data?.activeProfileId
        val chosenId = localPrefId?.takeIf { id -> pool.any { it.id == id } }
            ?: accountActiveId?.takeIf { id -> pool.any { it.id == id } }
        pool.firstOrNull { it.id == chosenId } ?: pool.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val discoverProfiles: StateFlow<UiState<List<VibeProfile>>> =
        repository.observeDiscoverProfiles(currentUserId).toUiStateFlow()

    val feedPosts: StateFlow<UiState<List<Post>>> =
        repository.observeFeedPosts(currentUserId).toUiStateFlow()

    val activeStories: StateFlow<UiState<List<Story>>> =
        repository.observeActiveStories(currentUserId).toUiStateFlow()

    val audioRooms: StateFlow<UiState<List<AudioRoom>>> =
        repository.observeAudioRooms(currentUserId).toUiStateFlow()

    val followingRelations: StateFlow<List<FollowRelation>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else repository.observeMyFollowingRelations(currentUserId, profile.id)
        }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<UiState<List<Conversation>>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else repository.observeConversations(currentUserId, profile.id)
        }
        .toUiStateFlow()

    val notifications: StateFlow<UiState<List<ProfileNotification>>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else repository.observeProfileNotifications(currentUserId, profile.id)
        }
        .toUiStateFlow()

    fun observePostLikes(postId: String): Flow<List<PostLike>> =
        repository.observePostLikes(currentUserId, postId).catch {
            if (it is CancellationException) throw it
            emit(emptyList())
        }

    fun observePostComments(postId: String): Flow<List<PostComment>> =
        repository.observePostComments(currentUserId, postId).catch {
            if (it is CancellationException) throw it
            emit(emptyList())
        }

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            profileStore.saveDarkTheme(isDark)
            repository.updateThemePreference(isDark)
        }
    }

    fun savePhoneAccountAndContinue(countryCode: String, phone: String, phoneVerified: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.upsertUserAccount(countryCode, phone, phoneVerified)
            _isBusy.value = false
            res.onFailure { _events.tryEmit(it.message ?: "Failed to save mobile number") }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean? {
        return repository.checkUsernameAvailable(username).getOrNull()
    }

    fun createProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarEmoji: String,
        avatarImage: String,
        cardTheme: String,
        moodStatus: String,
        moodEmoji: String,
        password: String = "",
        onCreated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createProfile(
                username = username,
                displayName = displayName,
                bio = bio,
                avatarEmoji = avatarEmoji,
                avatarImage = avatarImage,
                cardTheme = cardTheme,
                moodStatus = moodStatus,
                moodEmoji = moodEmoji,
                password = password
            )
            _isBusy.value = false
            res.onSuccess { newId ->
                profileStore.markProfileLoggedOut(currentUserId, newId, false)
                profileStore.saveActiveProfileId(currentUserId, newId)
                _events.tryEmit("Profile @$username ready!")
                onCreated()
            }.onFailure {
                _events.tryEmit(it.message ?: "Could not create profile")
            }
        }
    }

    fun loginWithUsernameAndPassword(
        username: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.loginWithUsernameAndPassword(username, password)
            _isBusy.value = false
            res.onSuccess { profile ->
                profileStore.markProfileLoggedOut(currentUserId, profile.id, false)
                profileStore.saveActiveProfileId(currentUserId, profile.id)
                _events.tryEmit("Logged in as @${profile.username}")
                onSuccess()
            }.onFailure {
                val msg = it.message ?: "Login failed"
                onError(msg)
                _events.tryEmit(msg)
            }
        }
    }

    fun logoutIndividualProfile(profile: VibeProfile) {
        viewModelScope.launch {
            _isBusy.value = true
            profileStore.markProfileLoggedOut(currentUserId, profile.id, true)
            val res = repository.logoutIndividualProfile(profile.id)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Logged out of @${profile.username}")
            }.onFailure {
                _events.tryEmit(it.message ?: "Logged out locally from @${profile.username}")
            }
        }
    }

    fun recoverPasswordWithMobile(
        username: String,
        newPassword: String,
        mobileDigits: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.recoverProfilePassword(username, newPassword, mobileDigits)
            _isBusy.value = false
            res.onSuccess { profile ->
                profileStore.markProfileLoggedOut(currentUserId, profile.id, false)
                profileStore.saveActiveProfileId(currentUserId, profile.id)
                _events.tryEmit("Password reset! Logged in as @${profile.username}")
                onSuccess()
            }.onFailure {
                val msg = it.message ?: "Password recovery failed"
                onError(msg)
                _events.tryEmit(msg)
            }
        }
    }

    fun switchProfile(profile: VibeProfile) {
        viewModelScope.launch {
            if (profile.id in loggedOutProfileIds.value) {
                _events.tryEmit("@${profile.username} is logged out. Enter password to log back in.")
                return@launch
            }
            if (profile.isDeactivated) {
                repository.setProfileDeactivated(profile.id, false)
            }
            profileStore.saveActiveProfileId(currentUserId, profile.id)
            repository.updateActiveProfileOnAccount(profile.id)
            _events.tryEmit("Switched to @${profile.username}")
        }
    }

    fun updateProfile(
        profileId: String,
        displayName: String,
        bio: String,
        cardTheme: String,
        avatarImage: String?,
        avatarEmoji: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateProfile(profileId, displayName, bio, cardTheme, avatarImage, avatarEmoji)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Profile updated")
                onDone()
            }.onFailure {
                _events.tryEmit(it.message ?: "Update failed")
            }
        }
    }

    fun updateMood(moodStatus: String, moodEmoji: String, activityStatus: String) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.updateMoodAndActivity(profile.id, moodStatus, moodEmoji, activityStatus)
                .onSuccess { _events.tryEmit("Mood set to $moodEmoji $moodStatus") }
                .onFailure { _events.tryEmit(it.message ?: "Failed to update mood") }
        }
    }

    fun toggleAnonymousDiscovery(enabled: Boolean) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.toggleAnonymousDiscovery(profile.id, enabled)
                .onSuccess {
                    _events.tryEmit(
                        if (enabled) "Anonymous Discovery enabled"
                        else "Anonymous Discovery turned off"
                    )
                }
        }
    }

    fun toggleProfileDeactivated(profile: VibeProfile) {
        viewModelScope.launch {
            val next = !profile.isDeactivated
            repository.setProfileDeactivated(profile.id, next)
                .onSuccess {
                    _events.tryEmit(
                        if (next) "@${profile.username} deactivated"
                        else "@${profile.username} reactivated"
                    )
                }
        }
    }

    fun deleteProfile(profile: VibeProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile.id)
                .onSuccess { _events.tryEmit("@${profile.username} deleted") }
                .onFailure { _events.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun createPost(
        caption: String,
        mediaType: String,
        mediaData: String,
        visibility: String,
        isReel: Boolean,
        audioTrack: String,
        onSuccess: () -> Unit
    ) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createPost(profile, caption, mediaType, mediaData, visibility, isReel, audioTrack)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit(if (isReel) "Reel published!" else "Post shared to VIBE!")
                onSuccess()
            }.onFailure {
                _events.tryEmit(it.message ?: "Failed to publish")
            }
        }
    }

    fun createStory(
        caption: String,
        mediaData: String,
        bgTheme: String,
        audience: String,
        onSuccess: () -> Unit
    ) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createStory(profile, caption, mediaData, bgTheme, audience)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("24h Story added!")
                onSuccess()
            }.onFailure {
                _events.tryEmit(it.message ?: "Failed to add story")
            }
        }
    }

    fun toggleLike(post: Post) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.toggleLike(post, profile)
        }
    }

    fun addComment(post: Post, text: String) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.addComment(post, profile, text)
                .onFailure { _events.tryEmit(it.message ?: "Comment failed") }
        }
    }

    fun sharePostToChat(post: Post, conversation: Conversation) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.incrementShareCount(post.id)
            repository.sendMessage(
                conversation = conversation,
                senderProfile = profile,
                text = "Check out @${post.authorUsername}'s vibe: \"${post.caption.take(120)}\"",
                mediaType = "post_share",
                mediaData = post.mediaData
            ).onSuccess {
                _events.tryEmit("Shared to chat!")
            }
        }
    }

    fun recordExternalShare(postId: String) {
        viewModelScope.launch {
            repository.incrementShareCount(postId)
        }
    }

    fun deletePost(post: Post) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.deletePost(post.id, profile.id)
                .onSuccess { _events.tryEmit("Post deleted") }
        }
    }

    fun recordStoryView(story: Story) {
        viewModelScope.launch {
            repository.incrementStoryView(story.id)
        }
    }

    fun toggleFollow(target: VibeProfile, isCurrentlyFollowing: Boolean) {
        val active = activeProfile.value ?: return
        if (active.id == target.id) return
        viewModelScope.launch {
            repository.toggleFollow(active, target, isCurrentlyFollowing)
                .onFailure { _events.tryEmit(it.message ?: "Action failed") }
        }
    }

    fun toggleCloseFriend(relation: FollowRelation) {
        viewModelScope.launch {
            repository.toggleCloseFriend(relation.id, !relation.isCloseFriend)
                .onSuccess {
                    _events.tryEmit(
                        if (!relation.isCloseFriend) "Added @${relation.targetUsername} to Close Friends"
                        else "Removed from Close Friends"
                    )
                }
        }
    }

    fun startDirectChat(target: VibeProfile, onOpenConversation: (String) -> Unit) {
        val active = activeProfile.value ?: return
        viewModelScope.launch {
            repository.getOrCreateDirectConversation(active, target)
                .onSuccess { convId -> onOpenConversation(convId) }
                .onFailure { _events.tryEmit(it.message ?: "Could not open chat") }
        }
    }

    fun createGroupChat(members: List<VibeProfile>, groupName: String, onOpenConversation: (String) -> Unit) {
        val active = activeProfile.value ?: return
        viewModelScope.launch {
            repository.createGroupConversation(active, members, groupName)
                .onSuccess { convId -> onOpenConversation(convId) }
                .onFailure { _events.tryEmit(it.message ?: "Could not create group") }
        }
    }

    fun createAudioRoom(title: String, topicTag: String) {
        val active = activeProfile.value ?: return
        viewModelScope.launch {
            repository.createAudioRoom(active, title, topicTag)
                .onSuccess { _events.tryEmit("Live Audio Room started!") }
                .onFailure { _events.tryEmit(it.message ?: "Could not start room") }
        }
    }

    fun joinRoom(room: AudioRoom) {
        viewModelScope.launch { repository.joinOrLeaveAudioRoom(room.id, 1L) }
    }

    fun leaveRoom(room: AudioRoom) {
        viewModelScope.launch { repository.joinOrLeaveAudioRoom(room.id, -1L) }
    }

    fun toggleRaiseHand(room: AudioRoom) {
        val active = activeProfile.value ?: return
        viewModelScope.launch { repository.toggleRaiseHand(room, active.username) }
    }

    fun promoteSpeaker(room: AudioRoom, handle: String) {
        viewModelScope.launch { repository.promoteSpeaker(room, handle) }
    }

    fun sendRoomReaction(room: AudioRoom, emoji: String) {
        viewModelScope.launch { repository.sendRoomReaction(room.id, emoji) }
    }

    fun endAudioRoom(room: AudioRoom) {
        viewModelScope.launch { repository.endAudioRoom(room.id) }
    }

    fun markNotificationRead(notif: ProfileNotification) {
        if (notif.isRead) return
        viewModelScope.launch { repository.markNotificationRead(notif.id) }
    }

    fun generateAiCaption(topic: String) {
        val mood = activeProfile.value?.moodStatus ?: "Vibing"
        viewModelScope.launch {
            _aiCaptionState.value = UiState.Loading
            aiRepository.generateCaptionAndHashtags(topic, mood)
                .onSuccess { _aiCaptionState.value = UiState.Success(it) }
                .onFailure { _aiCaptionState.value = UiState.Error(it.message ?: "VibeAI error") }
        }
    }
}
