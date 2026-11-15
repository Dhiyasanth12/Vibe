package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CountryCodeOption(
    val name: String,
    val dialCode: String,
    val flag: String
)

val SupportedCountryCodes = listOf(
    CountryCodeOption("India", "+91", "🇮🇳"),
    CountryCodeOption("United States", "+1", "🇺🇸"),
    CountryCodeOption("United Kingdom", "+44", "🇬🇧"),
    CountryCodeOption("United Arab Emirates", "+971", "🇦🇪"),
    CountryCodeOption("Singapore", "+65", "🇸🇬"),
    CountryCodeOption("Canada", "+1", "🇨🇦"),
    CountryCodeOption("Australia", "+61", "🇦🇺"),
    CountryCodeOption("Germany", "+49", "🇩🇪"),
    CountryCodeOption("Japan", "+81", "🇯🇵"),
    CountryCodeOption("Brazil", "+55", "🇧🇷")
)

val VibeMoodOptions = listOf(
    "Vibing" to "🎧",
    "Creating" to "✨",
    "Late Night" to "🌙",
    "On Air" to "🎙️",
    "Coffee Run" to "☕",
    "Focus Mode" to "⚡",
    "Lowkey" to "🫧"
)

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phoneCountryCode: String = "+91",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val activeProfileId: String = "",
    val profileCount: Long = 0L,
    @get:PropertyName("isDarkMode") @field:PropertyName("isDarkMode") val isDarkMode: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VibeProfile(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarImage: String = "",
    val avatarEmoji: String = "⚡",
    val cardTheme: String = "electric_purple",
    val moodStatus: String = "Vibing",
    val moodEmoji: String = "🎧",
    val activityStatus: String = "Online on VIBE",
    val anonymousDiscovery: Boolean = false,
    @get:PropertyName("isDeactivated") @field:PropertyName("isDeactivated") val isDeactivated: Boolean = false,
    @get:PropertyName("isOnline") @field:PropertyName("isOnline") val isOnline: Boolean = true,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val streakDays: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val profileId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatar: String = "",
    val authorCardTheme: String = "electric_purple",
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val mediaType: String = "photo",
    val mediaData: String = "",
    val visibility: String = "public",
    @get:PropertyName("isReel") @field:PropertyName("isReel") val isReel: Boolean = false,
    val audioTrack: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val postId: String = "",
    val ownerUid: String = "",
    val profileId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val ownerUid: String = "",
    val profileId: String = "",
    val authorUsername: String = "",
    val authorAvatar: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProfileCredential(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val profileId: String = "",
    val username: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val recoveryPhoneE164: String = "",
    @get:PropertyName("isLoggedOut") @field:PropertyName("isLoggedOut") val isLoggedOut: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UsernameReservation(
    @DocumentId val id: String = "",
    val username: String = "",
    val profileId: String = "",
    val ownerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val profileId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatar: String = "",
    val caption: String = "",
    val mediaData: String = "",
    val mediaRef: String = "",
    val sourcePostId: String = "",
    val bgTheme: String = "electric_purple",
    val audience: String = "all",
    val expiresAtMillis: Long = 0L,
    val viewsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRelation(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val targetOwnerUid: String = "",
    val followerProfileId: String = "",
    val targetProfileId: String = "",
    val targetUsername: String = "",
    val targetDisplayName: String = "",
    @get:PropertyName("isCloseFriend") @field:PropertyName("isCloseFriend") val isCloseFriend: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val createdByUid: String = "",
    val participantUids: List<String> = emptyList(),
    val participantProfileIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    @get:PropertyName("isGroup") @field:PropertyName("isGroup") val isGroup: Boolean = false,
    val groupName: String = "",
    val chatTheme: String = "midnight_purple",
    val disappearingSeconds: Long = 0L,
    val lastMessage: String = "",
    val lastMessageSenderProfileId: String = "",
    val typingProfileIds: List<String> = emptyList(),
    val streakCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantUids: List<String> = emptyList(),
    val senderUid: String = "",
    val senderProfileId: String = "",
    val senderName: String = "",
    val text: String = "",
    val mediaType: String = "text",
    val mediaData: String = "",
    val voiceDurationSec: Long = 0L,
    val replyToMessageId: String = "",
    val replyToPreview: String = "",
    val forwardedFrom: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val readByProfileIds: List<String> = emptyList(),
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AudioRoom(
    @DocumentId val id: String = "",
    val hostUid: String = "",
    val hostProfileId: String = "",
    val hostUsername: String = "",
    val title: String = "",
    val topicTag: String = "",
    val speakerNames: List<String> = emptyList(),
    val raisedHands: List<String> = emptyList(),
    val listenerCount: Long = 1L,
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    val latestReaction: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProfileNotification(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorProfileId: String = "",
    val actorUsername: String = "",
    val targetOwnerUid: String = "",
    val targetProfileId: String = "",
    val type: String = "like",
    val message: String = "",
    @get:PropertyName("isRead") @field:PropertyName("isRead") val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
