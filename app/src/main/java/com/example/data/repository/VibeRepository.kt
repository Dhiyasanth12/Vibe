package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AudioRoom
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FollowRelation
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostLike
import com.example.data.model.ProfileCredential
import com.example.data.model.ProfileNotification
import com.example.data.model.Story
import com.example.data.model.UserAccount
import com.example.data.model.VibeProfile
import com.example.util.OperationType
import com.example.util.PasswordSecurity
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VibeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

    private fun <T> Flow<T>.wrapFirestoreCatch(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    // ==================== USER ACCOUNT (PHONE & MULTI-PROFILE ROOT) ====================

    fun observeUserAccount(uid: String): Flow<UserAccount?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreCatch(OperationType.GET, path, uid)
        )
    }

    suspend fun upsertUserAccount(
        phoneCountryCode: String,
        phoneNumber: String,
        phoneVerified: Boolean,
        activeProfileId: String = "",
        isDarkMode: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "phoneCountryCode" to phoneCountryCode,
                "phoneNumber" to phoneNumber,
                "phoneVerified" to phoneVerified,
                "activeProfileId" to activeProfileId,
                "profileCount" to 0L,
                "isDarkMode" to isDarkMode,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updates = mapOf(
                "phoneCountryCode" to phoneCountryCode,
                "phoneNumber" to phoneNumber,
                "phoneVerified" to phoneVerified,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    suspend fun updateActiveProfileOnAccount(profileId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        docRef.set(
            mapOf(
                "activeProfileId" to profileId,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    suspend fun updateThemePreference(isDarkMode: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "isDarkMode" to isDarkMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    // ==================== MULTI-PROFILE SYSTEM ====================

    fun observeMyProfiles(uid: String): Flow<List<VibeProfile>> = flow {
        val path = "profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VibeProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeDiscoverProfiles(uid: String): Flow<List<VibeProfile>> = flow {
        val path = "profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("isDeactivated", false)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VibeProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.followersCount }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeMyProfileCredentials(uid: String): Flow<List<ProfileCredential>> = flow {
        val path = "profile_credentials"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ProfileCredential::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun checkUsernameAvailable(username: String): Result<Boolean> = runCatching {
        requireUserId()
        val clean = username.trim().lowercase().replace("@", "").replace(" ", "_")
        if (clean.length !in 3..30 || !Regex("^[a-z0-9_.]+$").matches(clean)) {
            return@runCatching false
        }
        val reserved = db.collection("usernames").document(clean).get().await()
        if (reserved.exists()) return@runCatching false
        val legacyProfiles = db.collection("profiles")
            .whereEqualTo("isDeactivated", false)
            .whereEqualTo("username", clean)
            .get()
            .await()
        legacyProfiles.isEmpty
    }

    suspend fun createProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarEmoji: String,
        avatarImage: String,
        cardTheme: String,
        moodStatus: String,
        moodEmoji: String,
        password: String = "",
        recoveryPhoneE164: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanUsername = username.trim().lowercase().replace("@", "").replace(" ", "_")
        require(cleanUsername.length in 3..30) { "Username must be 3-30 characters." }
        require(Regex("^[a-z0-9_.]+$").matches(cleanUsername)) {
            "Username can only contain lowercase letters, numbers, underscores, and dots."
        }
        if (password.isNotEmpty()) {
            require(password.length >= 6) { "Password must be at least 6 characters." }
        }

        // Enforce platform abuse limit (max 10 profiles per verified account)
        val existingMyProfiles = db.collection("profiles")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()
        require(existingMyProfiles.size() < 10) {
            "Maximum limit of 10 profiles per mobile account reached."
        }

        // Check username uniqueness in atomic registry and active profiles
        val reservationRef = db.collection("usernames").document(cleanUsername)
        val existingReservation = reservationRef.get().await()
        require(!existingReservation.exists()) { "Username @$cleanUsername is already taken." }

        val dupCheck = db.collection("profiles")
            .whereEqualTo("isDeactivated", false)
            .whereEqualTo("username", cleanUsername)
            .get()
            .await()
        require(dupCheck.isEmpty) { "Username @$cleanUsername is already taken." }

        val profileId = "prof_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val profileRef = db.collection("profiles").document(profileId)
        val credRef = db.collection("profile_credentials").document(profileId)
        val userRef = db.collection("users").document(uid)
        val userDoc = userRef.get().await()

        val resolvedRecoveryPhone = recoveryPhoneE164.ifBlank {
            val cc = userDoc.getString("phoneCountryCode").orEmpty()
            val num = userDoc.getString("phoneNumber").orEmpty()
            if (num.isNotBlank()) "$cc$num" else ""
        }.take(24)

        val hashed = if (password.isNotEmpty()) {
            PasswordSecurity.hashPassword(password)
        } else {
            PasswordSecurity.HashedPassword(hashBase64 = "", saltBase64 = "")
        }

        val payload = hashMapOf<String, Any>(
            "ownerUid" to uid,
            "username" to cleanUsername,
            "displayName" to displayName.trim().take(50),
            "bio" to bio.trim().take(240),
            "avatarEmoji" to avatarEmoji.ifBlank { "⚡" },
            "cardTheme" to cardTheme,
            "moodStatus" to moodStatus,
            "moodEmoji" to moodEmoji,
            "activityStatus" to "Online on VIBE",
            "anonymousDiscovery" to false,
            "isDeactivated" to false,
            "isOnline" to true,
            "followersCount" to 0L,
            "followingCount" to 0L,
            "postsCount" to 0L,
            "streakDays" to 1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (avatarImage.isNotBlank()) {
            payload["avatarImage"] = avatarImage
        }

        val credentialPayload = hashMapOf<String, Any>(
            "ownerUid" to uid,
            "profileId" to profileId,
            "username" to cleanUsername,
            "passwordHash" to hashed.hashBase64,
            "passwordSalt" to hashed.saltBase64,
            "recoveryPhoneE164" to resolvedRecoveryPhone,
            "isLoggedOut" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val usernamePayload = hashMapOf<String, Any>(
            "username" to cleanUsername,
            "profileId" to profileId,
            "ownerUid" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(profileRef, payload)
        batch.set(credRef, credentialPayload)
        batch.set(reservationRef, usernamePayload)
        if (userDoc.exists()) {
            batch.update(
                userRef,
                mapOf(
                    "activeProfileId" to profileId,
                    "profileCount" to (existingMyProfiles.size() + 1).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "phoneCountryCode" to "+91",
                    "phoneNumber" to "",
                    "phoneVerified" to false,
                    "activeProfileId" to profileId,
                    "profileCount" to 1L,
                    "isDarkMode" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        profileId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "profiles")
    }

    suspend fun loginWithUsernameAndPassword(
        username: String,
        password: String
    ): Result<VibeProfile> = runCatching {
        val uid = requireUserId()
        val clean = username.trim().lowercase().replace("@", "")
        require(clean.length in 3..30) { "Enter a valid username." }
        require(password.length >= 6) { "Enter your password (at least 6 characters)." }

        val myProfiles = db.collection("profiles")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()
            .toObjects(VibeProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        val matched = myProfiles.firstOrNull { it.username == clean }
        if (matched == null) {
            val reservation = db.collection("usernames").document(clean).get().await()
            if (reservation.exists()) {
                throw IllegalArgumentException("@$clean belongs to a different verified mobile/Google identity.")
            } else {
                throw IllegalArgumentException("No VIBE account found for @$clean.")
            }
        }

        val credSnap = db.collection("profile_credentials").document(matched.id).get().await()
        val cred = credSnap.toObject(ProfileCredential::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        if (cred != null && cred.passwordHash.isNotBlank()) {
            require(PasswordSecurity.verifyPassword(password, cred.passwordHash, cred.passwordSalt)) {
                "Incorrect password for @$clean."
            }
            if (cred.isLoggedOut) {
                db.collection("profile_credentials").document(matched.id).update(
                    mapOf(
                        "isLoggedOut" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } else {
            // Legacy profile without password yet -> bind this password securely on first login
            val hashed = PasswordSecurity.hashPassword(password)
            db.collection("profile_credentials").document(matched.id).set(
                mapOf(
                    "ownerUid" to uid,
                    "profileId" to matched.id,
                    "username" to clean,
                    "passwordHash" to hashed.hashBase64,
                    "passwordSalt" to hashed.saltBase64,
                    "isLoggedOut" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        db.collection("users").document(uid).set(
            mapOf(
                "userId" to uid,
                "activeProfileId" to matched.id,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        matched
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "profile_credentials")
    }

    suspend fun logoutIndividualProfile(profileId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val credRef = db.collection("profile_credentials").document(profileId)
        val credSnap = credRef.get().await()
        if (credSnap.exists()) {
            credRef.update(
                mapOf(
                    "isLoggedOut" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "profile_credentials/$profileId")
    }

    suspend fun recoverProfilePassword(
        username: String,
        newPassword: String,
        mobileDigitsInput: String
    ): Result<VibeProfile> = runCatching {
        val uid = requireUserId()
        val clean = username.trim().lowercase().replace("@", "")
        require(clean.length in 3..30) { "Enter a valid username." }
        require(newPassword.length >= 6) { "New password must be at least 6 characters." }
        val cleanInputDigits = mobileDigitsInput.filter(Char::isDigit)
        require(cleanInputDigits.length >= 7) { "Enter your verified mobile number." }

        val userDoc = db.collection("users").document(uid).get().await()
        val savedNumber = userDoc.getString("phoneNumber").orEmpty().filter(Char::isDigit)
        val savedFull = (userDoc.getString("phoneCountryCode").orEmpty() + savedNumber).filter(Char::isDigit)
        require(savedNumber.isNotBlank() && (cleanInputDigits == savedNumber || cleanInputDigits == savedFull)) {
            "Mobile number does not match the verified recovery number on this account."
        }

        val myProfiles = db.collection("profiles")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()
            .toObjects(VibeProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val target = myProfiles.firstOrNull { it.username == clean }
            ?: throw IllegalArgumentException("No VIBE account @$clean found under this mobile number.")

        val hashed = PasswordSecurity.hashPassword(newPassword)
        val credRef = db.collection("profile_credentials").document(target.id)
        val existingCred = credRef.get().await()
        val resolvedRecoveryPhone = "${userDoc.getString("phoneCountryCode").orEmpty()}$savedNumber".take(24)
        if (existingCred.exists()) {
            credRef.update(
                mapOf(
                    "passwordHash" to hashed.hashBase64,
                    "passwordSalt" to hashed.saltBase64,
                    "recoveryPhoneE164" to resolvedRecoveryPhone,
                    "isLoggedOut" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            credRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "profileId" to target.id,
                    "username" to clean,
                    "passwordHash" to hashed.hashBase64,
                    "passwordSalt" to hashed.saltBase64,
                    "recoveryPhoneE164" to resolvedRecoveryPhone,
                    "isLoggedOut" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        db.collection("users").document(uid).update(
            mapOf(
                "activeProfileId" to target.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        target
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "profile_credentials")
    }

    suspend fun updateProfile(
        profileId: String,
        displayName: String,
        bio: String,
        cardTheme: String,
        avatarImage: String?,
        avatarEmoji: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val updates = mutableMapOf<String, Any>(
            "displayName" to displayName.trim().take(50),
            "bio" to bio.trim().take(240),
            "cardTheme" to cardTheme,
            "avatarEmoji" to avatarEmoji,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarImage.isNullOrBlank()) {
            updates["avatarImage"] = avatarImage
        }
        db.collection("profiles").document(profileId).update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "profiles/$profileId")
    }

    suspend fun updateMoodAndActivity(
        profileId: String,
        moodStatus: String,
        moodEmoji: String,
        activityStatus: String
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("profiles").document(profileId).update(
            mapOf(
                "moodStatus" to moodStatus,
                "moodEmoji" to moodEmoji,
                "activityStatus" to activityStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "profiles/$profileId")
    }

    suspend fun toggleAnonymousDiscovery(profileId: String, enabled: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("profiles").document(profileId).update(
            mapOf(
                "anonymousDiscovery" to enabled,
                "activityStatus" to if (enabled) "Exploring incognito" else "Online on VIBE",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "profiles/$profileId")
    }

    suspend fun setProfileDeactivated(profileId: String, deactivated: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("profiles").document(profileId).update(
            mapOf(
                "isDeactivated" to deactivated,
                "isOnline" to !deactivated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "profiles/$profileId")
    }

    suspend fun deleteProfile(profileId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val myProfiles = db.collection("profiles").whereEqualTo("ownerUid", uid).get().await()
        val targetDoc = myProfiles.documents.firstOrNull { it.id == profileId }
        val targetUsername = targetDoc?.getString("username").orEmpty()
        val remaining = myProfiles.documents.filter { it.id != profileId }
        val nextActiveId = remaining.firstOrNull { !(it.getBoolean("isDeactivated") ?: false) }?.id
            ?: remaining.firstOrNull()?.id
            ?: ""

        val batch = db.batch()
        batch.delete(db.collection("profiles").document(profileId))
        batch.delete(db.collection("profile_credentials").document(profileId))
        if (targetUsername.isNotBlank()) {
            batch.delete(db.collection("usernames").document(targetUsername))
        }
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "profileCount" to remaining.size.toLong(),
                "activeProfileId" to nextActiveId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "profiles/$profileId")
    }

    // ==================== SOCIAL FEED, POSTS, REELS & INTERACTIONS ====================

    fun observeFeedPosts(uid: String): Flow<List<Post>> = flow {
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereIn("visibility", listOf("public", "close_friends"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeProfilePosts(uid: String, profileId: String): Flow<List<Post>> = flow {
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("profileId", profileId)
                .whereIn("visibility", listOf("public", "close_friends"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun createPost(
        profile: VibeProfile,
        caption: String,
        mediaType: String,
        mediaData: String,
        visibility: String,
        isReel: Boolean,
        audioTrack: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val hashtags = Regex("#([A-Za-z0-9_]+)")
            .findAll(caption)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .take(10)
            .toList()

        val payload = hashMapOf<String, Any>(
            "ownerUid" to uid,
            "profileId" to profile.id,
            "authorUsername" to profile.username,
            "authorDisplayName" to profile.displayName,
            "authorCardTheme" to profile.cardTheme,
            "caption" to caption.trim().take(1000),
            "hashtags" to hashtags,
            "mediaType" to mediaType,
            "visibility" to visibility,
            "isReel" to isReel,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "sharesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (profile.avatarImage.isNotBlank()) payload["authorAvatar"] = profile.avatarImage
        if (mediaData.isNotBlank()) payload["mediaData"] = mediaData
        if (audioTrack.isNotBlank()) payload["audioTrack"] = audioTrack.take(100)

        val batch = db.batch()
        batch.set(db.collection("posts").document(postId), payload)
        batch.update(
            db.collection("profiles").document(profile.id),
            mapOf(
                "postsCount" to FieldValue.increment(1L),
                "streakDays" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        postId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts")
    }

    fun observePostLikes(uid: String, postId: String): Flow<List<PostLike>> = flow {
        val path = "posts/$postId/likes"
        emitAll(
            db.collection("posts").document(postId).collection("likes")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostLike::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun toggleLike(post: Post, activeProfile: VibeProfile): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val likeRef = db.collection("posts").document(post.id)
            .collection("likes").document(activeProfile.id)
        val postRef = db.collection("posts").document(post.id)
        val existingLikes = db.collection("posts").document(post.id)
            .collection("likes")
            .whereEqualTo("postId", post.id)
            .get()
            .await()
        val alreadyLiked = existingLikes.documents.any { it.id == activeProfile.id }

        val batch = db.batch()
        val likedNow: Boolean
        if (alreadyLiked) {
            batch.delete(likeRef)
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to (post.likesCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            likedNow = false
        } else {
            batch.set(
                likeRef,
                mapOf(
                    "postId" to post.id,
                    "ownerUid" to uid,
                    "profileId" to activeProfile.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            likedNow = true
        }
        batch.commit().await()

        if (likedNow && post.profileId != activeProfile.id) {
            createNotification(
                actorProfile = activeProfile,
                targetOwnerUid = post.ownerUid,
                targetProfileId = post.profileId,
                type = "like",
                message = "@${activeProfile.username} liked your vibe"
            )
        }
        likedNow
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/likes")
    }

    fun observePostComments(uid: String, postId: String): Flow<List<PostComment>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun addComment(post: Post, activeProfile: VibeProfile, text: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanText = text.trim().take(500)
        require(cleanText.isNotBlank())
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val commentRef = db.collection("posts").document(post.id).collection("comments").document(commentId)
        val postRef = db.collection("posts").document(post.id)

        val payload = hashMapOf<String, Any>(
            "postId" to post.id,
            "ownerUid" to uid,
            "profileId" to activeProfile.id,
            "authorUsername" to activeProfile.username,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (activeProfile.avatarImage.isNotBlank()) payload["authorAvatar"] = activeProfile.avatarImage

        val batch = db.batch()
        batch.set(commentRef, payload)
        batch.update(
            postRef,
            mapOf(
                "commentsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()

        if (post.profileId != activeProfile.id) {
            createNotification(
                actorProfile = activeProfile,
                targetOwnerUid = post.ownerUid,
                targetProfileId = post.profileId,
                type = "comment",
                message = "@${activeProfile.username} commented: \"${cleanText.take(40)}\""
            )
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
    }

    suspend fun incrementShareCount(postId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("posts").document(postId).update(
            mapOf(
                "sharesCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
    }

    suspend fun deletePost(postId: String, profileId: String): Result<Unit> = runCatching {
        requireUserId()
        val batch = db.batch()
        batch.delete(db.collection("posts").document(postId))
        batch.update(
            db.collection("profiles").document(profileId),
            mapOf(
                "postsCount" to FieldValue.increment(-1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
    }

    // ==================== 24-HOUR DISAPPEARING STORIES ====================

    fun observeActiveStories(uid: String): Flow<List<Story>> = flow {
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereIn("audience", listOf("all", "close_friends"))
                .snapshots()
                .map { snap ->
                    val now = System.currentTimeMillis()
                    snap.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.expiresAtMillis > now }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun createStory(
        profile: VibeProfile,
        caption: String,
        mediaData: String,
        bgTheme: String,
        audience: String,
        mediaRef: String = "",
        sourcePostId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val storyId = "str_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        val payload = hashMapOf<String, Any>(
            "ownerUid" to uid,
            "profileId" to profile.id,
            "authorUsername" to profile.username,
            "authorDisplayName" to profile.displayName,
            "caption" to caption.trim().take(300),
            "bgTheme" to bgTheme,
            "audience" to audience,
            "expiresAtMillis" to expiresAt,
            "viewsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (profile.avatarImage.isNotBlank()) payload["authorAvatar"] = profile.avatarImage
        if (mediaData.isNotBlank()) payload["mediaData"] = mediaData
        if (mediaRef.isNotBlank()) payload["mediaRef"] = mediaRef.take(300)
        if (sourcePostId.isNotBlank()) payload["sourcePostId"] = sourcePostId.take(128)
        db.collection("stories").document(storyId).set(payload).await()
        storyId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "stories")
    }

    suspend fun incrementStoryView(storyId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("stories").document(storyId).update(
            mapOf(
                "viewsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "stories/$storyId")
    }

    // ==================== FOLLOWS & CLOSE FRIENDS ====================

    fun observeMyFollowingRelations(uid: String, followerProfileId: String): Flow<List<FollowRelation>> = flow {
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.followerProfileId == followerProfileId }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun toggleFollow(
        activeProfile: VibeProfile,
        targetProfile: VibeProfile,
        currentlyFollowing: Boolean
    ): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val followId = "${activeProfile.id}_${targetProfile.id}"
        val followRef = db.collection("follows").document(followId)
        val batch = db.batch()
        if (currentlyFollowing) {
            batch.delete(followRef)
            batch.update(
                db.collection("profiles").document(activeProfile.id),
                mapOf(
                    "followingCount" to (activeProfile.followingCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("profiles").document(targetProfile.id),
                mapOf(
                    "followersCount" to (targetProfile.followersCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                followRef,
                mapOf(
                    "ownerUid" to uid,
                    "targetOwnerUid" to targetProfile.ownerUid,
                    "followerProfileId" to activeProfile.id,
                    "targetProfileId" to targetProfile.id,
                    "targetUsername" to targetProfile.username,
                    "targetDisplayName" to targetProfile.displayName,
                    "isCloseFriend" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("profiles").document(activeProfile.id),
                mapOf(
                    "followingCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("profiles").document(targetProfile.id),
                mapOf(
                    "followersCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()

        if (!currentlyFollowing && targetProfile.id != activeProfile.id) {
            createNotification(
                actorProfile = activeProfile,
                targetOwnerUid = targetProfile.ownerUid,
                targetProfileId = targetProfile.id,
                type = "follow",
                message = "@${activeProfile.username} started following you"
            )
        }
        !currentlyFollowing
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "follows")
    }

    suspend fun toggleCloseFriend(followId: String, isCloseFriend: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("follows").document(followId).update(
            mapOf(
                "isCloseFriend" to isCloseFriend,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "follows/$followId")
    }

    // ==================== REAL-TIME CONVERSATIONS & MESSAGES ====================

    fun observeConversations(uid: String, activeProfileId: String): Flow<List<Conversation>> = flow {
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { activeProfileId in it.participantProfileIds }
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun getOrCreateDirectConversation(
        activeProfile: VibeProfile,
        targetProfile: VibeProfile
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sortedProfiles = listOf(activeProfile.id, targetProfile.id).sorted()
        val convId = "dm_${sortedProfiles.joinToString("_")}"
        val convRef = db.collection("conversations").document(convId)
        val existingConvs = db.collection("conversations")
            .whereArrayContains("participantUids", uid)
            .get()
            .await()
        if (existingConvs.documents.none { it.id == convId }) {
            val participantUids = listOf(uid, targetProfile.ownerUid).distinct()
            val participantNames = mapOf(
                activeProfile.id to activeProfile.displayName,
                targetProfile.id to targetProfile.displayName
            )
            convRef.set(
                mapOf(
                    "createdByUid" to uid,
                    "participantUids" to participantUids,
                    "participantProfileIds" to sortedProfiles,
                    "participantNames" to participantNames,
                    "isGroup" to false,
                    "groupName" to "",
                    "chatTheme" to "midnight_purple",
                    "disappearingSeconds" to 0L,
                    "lastMessage" to "Say hi on VIBE ✨",
                    "lastMessageSenderProfileId" to activeProfile.id,
                    "typingProfileIds" to emptyList<String>(),
                    "streakCount" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        convId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "conversations")
    }

    suspend fun createGroupConversation(
        activeProfile: VibeProfile,
        memberProfiles: List<VibeProfile>,
        groupName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val allProfiles = (listOf(activeProfile) + memberProfiles).distinctBy { it.id }
        val convId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val participantUids = allProfiles.map { it.ownerUid }.distinct()
        val participantProfileIds = allProfiles.map { it.id }
        val participantNames = allProfiles.associate { it.id to it.displayName }

        db.collection("conversations").document(convId).set(
            mapOf(
                "createdByUid" to uid,
                "participantUids" to participantUids,
                "participantProfileIds" to participantProfileIds,
                "participantNames" to participantNames,
                "isGroup" to true,
                "groupName" to groupName.trim().take(60),
                "chatTheme" to "midnight_purple",
                "disappearingSeconds" to 0L,
                "lastMessage" to "${activeProfile.displayName} created the group",
                "lastMessageSenderProfileId" to activeProfile.id,
                "typingProfileIds" to emptyList<String>(),
                "streakCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        convId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "conversations")
    }

    fun observeMessages(uid: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    val now = System.currentTimeMillis()
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.expiresAtMillis == 0L || it.expiresAtMillis > now }
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderProfile: VibeProfile,
        text: String,
        mediaType: String = "text",
        mediaData: String = "",
        voiceDurationSec: Long = 0L,
        replyTo: ChatMessage? = null,
        forwardedFrom: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)

        val expiresAt = if (conversation.disappearingSeconds > 0L) {
            System.currentTimeMillis() + conversation.disappearingSeconds * 1000L
        } else 0L

        val payload = hashMapOf<String, Any>(
            "conversationId" to conversation.id,
            "participantUids" to conversation.participantUids,
            "senderUid" to uid,
            "senderProfileId" to senderProfile.id,
            "senderName" to senderProfile.displayName,
            "text" to text.trim().take(2000),
            "mediaType" to mediaType,
            "voiceDurationSec" to voiceDurationSec,
            "reactions" to emptyMap<String, String>(),
            "readByProfileIds" to listOf(senderProfile.id),
            "expiresAtMillis" to expiresAt,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (mediaData.isNotBlank()) payload["mediaData"] = mediaData
        if (replyTo != null) {
            payload["replyToMessageId"] = replyTo.id
            payload["replyToPreview"] = "${replyTo.senderName}: ${replyTo.text.ifBlank { replyTo.mediaType }}".take(160)
        }
        if (forwardedFrom.isNotBlank()) payload["forwardedFrom"] = forwardedFrom.take(60)

        val preview = when (mediaType) {
            "image" -> "📷 Photo"
            "video" -> "🎥 Video"
            "voice" -> "🎤 Voice note (${voiceDurationSec}s)"
            "call_event" -> text.take(80)
            "post_share" -> "✨ Shared a post"
            else -> text.trim().take(120)
        }

        val batch = db.batch()
        batch.set(msgRef, payload)
        batch.update(
            convRef,
            mapOf(
                "lastMessage" to preview,
                "lastMessageSenderProfileId" to senderProfile.id,
                "typingProfileIds" to conversation.typingProfileIds.filter { it != senderProfile.id },
                "streakCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "conversations/${conversation.id}/messages")
    }

    suspend fun updateTypingStatus(
        conversation: Conversation,
        profileId: String,
        isTyping: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val updated = if (isTyping) {
            (conversation.typingProfileIds + profileId).distinct().take(20)
        } else {
            conversation.typingProfileIds.filter { it != profileId }
        }
        if (updated == conversation.typingProfileIds) return@runCatching
        db.collection("conversations").document(conversation.id).update(
            mapOf(
                "typingProfileIds" to updated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}")
    }

    suspend fun markMessagesAsRead(
        conversationId: String,
        messages: List<ChatMessage>,
        readerProfileId: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val unread = messages.filter {
            it.senderProfileId != readerProfileId && readerProfileId !in it.readByProfileIds
        }
        if (unread.isEmpty()) return@runCatching
        val batch = db.batch()
        unread.take(25).forEach { msg ->
            val msgRef = db.collection("conversations").document(conversationId)
                .collection("messages").document(msg.id)
            batch.update(
                msgRef,
                mapOf(
                    "readByProfileIds" to (msg.readByProfileIds + readerProfileId).distinct().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages")
    }

    suspend fun toggleMessageReaction(
        conversationId: String,
        message: ChatMessage,
        profileId: String,
        emoji: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val updated = message.reactions.toMutableMap()
        if (updated[profileId] == emoji) updated.remove(profileId) else updated[profileId] = emoji
        db.collection("conversations").document(conversationId)
            .collection("messages").document(message.id)
            .update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/${message.id}")
    }

    suspend fun updateChatTheme(conversationId: String, chatTheme: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("conversations").document(conversationId).update(
            mapOf(
                "chatTheme" to chatTheme,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId")
    }

    suspend fun updateDisappearingTimer(conversationId: String, seconds: Long): Result<Unit> = runCatching {
        requireUserId()
        db.collection("conversations").document(conversationId).update(
            mapOf(
                "disappearingSeconds" to seconds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId")
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "conversations/$conversationId/messages/$messageId")
    }

    // ==================== LIVE AUDIO HANGOUT ROOMS ====================

    fun observeAudioRooms(uid: String): Flow<List<AudioRoom>> = flow {
        val path = "audio_rooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AudioRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.listenerCount }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun createAudioRoom(
        hostProfile: VibeProfile,
        title: String,
        topicTag: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        db.collection("audio_rooms").document(roomId).set(
            mapOf(
                "hostUid" to uid,
                "hostProfileId" to hostProfile.id,
                "hostUsername" to hostProfile.username,
                "title" to title.trim().take(100),
                "topicTag" to topicTag.trim().take(30),
                "speakerNames" to listOf("@${hostProfile.username}"),
                "raisedHands" to emptyList<String>(),
                "listenerCount" to 1L,
                "isActive" to true,
                "latestReaction" to "🎙️",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        roomId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "audio_rooms")
    }

    suspend fun toggleRaiseHand(room: AudioRoom, username: String): Result<Unit> = runCatching {
        requireUserId()
        val handle = "@$username"
        val updated = if (handle in room.raisedHands) {
            room.raisedHands - handle
        } else {
            (room.raisedHands + handle).distinct().take(30)
        }
        db.collection("audio_rooms").document(room.id).update(
            mapOf(
                "raisedHands" to updated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "audio_rooms/${room.id}")
    }

    suspend fun promoteSpeaker(room: AudioRoom, handle: String): Result<Unit> = runCatching {
        requireUserId()
        val speakers = (room.speakerNames + handle).distinct().take(20)
        val hands = room.raisedHands - handle
        db.collection("audio_rooms").document(room.id).update(
            mapOf(
                "speakerNames" to speakers,
                "raisedHands" to hands,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "audio_rooms/${room.id}")
    }

    suspend fun sendRoomReaction(roomId: String, emoji: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("audio_rooms").document(roomId).update(
            mapOf(
                "latestReaction" to emoji,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "audio_rooms/$roomId")
    }

    suspend fun joinOrLeaveAudioRoom(roomId: String, delta: Long): Result<Unit> = runCatching {
        requireUserId()
        db.collection("audio_rooms").document(roomId).update(
            mapOf(
                "listenerCount" to FieldValue.increment(delta),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "audio_rooms/$roomId")
    }

    suspend fun endAudioRoom(roomId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("audio_rooms").document(roomId).update(
            mapOf(
                "isActive" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "audio_rooms/$roomId")
    }

    // ==================== PROFILE-ISOLATED NOTIFICATIONS ====================

    fun observeProfileNotifications(uid: String, profileId: String): Flow<List<ProfileNotification>> = flow {
        val path = "notifications"
        emitAll(
            db.collection(path)
                .whereEqualTo("targetOwnerUid", uid)
                .whereEqualTo("targetProfileId", profileId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ProfileNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun createNotification(
        actorProfile: VibeProfile,
        targetOwnerUid: String,
        targetProfileId: String,
        type: String,
        message: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val notifId = "ntf_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        runCatching {
            db.collection("notifications").document(notifId).set(
                mapOf(
                    "actorUid" to uid,
                    "actorProfileId" to actorProfile.id,
                    "actorUsername" to actorProfile.username,
                    "targetOwnerUid" to targetOwnerUid,
                    "targetProfileId" to targetProfileId,
                    "type" to type,
                    "message" to message.take(240),
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { e ->
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.CREATE, "notifications/$notifId")
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("notifications").document(notificationId).update(
            mapOf(
                "isRead" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
    }
}
