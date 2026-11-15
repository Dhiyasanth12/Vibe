package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VibeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createMultipleProfiles_underOneVerifiedAccount_succeedsAndIsolatesData() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@vibe.test")
        val repo = VibeRepository(firestore, auth)

        val accountRes = withTimeout(TIMEOUT_MS) {
            repo.upsertUserAccount(phoneCountryCode = "+91", phoneNumber = "9876543210", phoneVerified = true)
        }
        assertTrue(accountRes.isSuccess)

        val suffix = UUID.randomUUID().toString().replace("-", "").take(6)
        val p1 = withTimeout(TIMEOUT_MS) {
            repo.createProfile(
                username = "alice_$suffix",
                displayName = "Alice Main",
                bio = "Midnight studio",
                avatarEmoji = "⚡",
                avatarImage = "",
                cardTheme = "electric_purple",
                moodStatus = "Vibing",
                moodEmoji = "🎧"
            ).getOrThrow()
        }

        val p2 = withTimeout(TIMEOUT_MS) {
            repo.createProfile(
                username = "alice_alt_$suffix",
                displayName = "Alice Alt",
                bio = "Photography",
                avatarEmoji = "🌙",
                avatarImage = "",
                cardTheme = "cyber_cyan",
                moodStatus = "Creating",
                moodEmoji = "✨"
            ).getOrThrow()
        }

        val myProfiles = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyProfiles(uid).first { list -> list.any { it.id == p1 } && list.any { it.id == p2 } }
        }
        assertTrue(myProfiles.size >= 2)
    }

    @Test
    fun crossUser_privateAccountAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@vibe.test")
        val aliceRepo = VibeRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) {
            aliceRepo.upsertUserAccount("+91", "9876543210", true).getOrThrow()
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@vibe.test")
        val bobRepo = VibeRepository(firestore, auth)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserAccount(aliceUid).first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull("Expected PERMISSION_DENIED for cross-user account access", captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun unauthenticatedCaller_observeProfiles_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = VibeRepository(firestore, auth)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDiscoverProfiles("nobody").first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull("Expected PERMISSION_DENIED for unauthenticated query", captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun postAndComment_andDirectChat_roundTripSucceeds() = runBlocking {
        val aliceUid = signInTestUser("alice_chat_${UUID.randomUUID()}@vibe.test")
        val repo = VibeRepository(firestore, auth)
        val suffix = UUID.randomUUID().toString().replace("-", "").take(6)
        val aliceProfileId = withTimeout(TIMEOUT_MS) {
            repo.createProfile(
                username = "a_chat_$suffix",
                displayName = "Alice Chat",
                bio = "Hi",
                avatarEmoji = "⚡",
                avatarImage = "",
                cardTheme = "electric_purple",
                moodStatus = "Vibing",
                moodEmoji = "🎧"
            ).getOrThrow()
        }
        val aliceProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyProfiles(aliceUid).first { it.any { p -> p.id == aliceProfileId } }
                .first { it.id == aliceProfileId }
        }

        val postId = withTimeout(TIMEOUT_MS) {
            repo.createPost(
                profile = aliceProfile,
                caption = "Electric nights #vibe",
                mediaType = "text",
                mediaData = "",
                visibility = "public",
                isReel = false,
                audioTrack = ""
            ).getOrThrow()
        }
        val feed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeFeedPosts(aliceUid).first { list -> list.any { it.id == postId } }
        }
        val createdPost = feed.first { it.id == postId }
        val likedNow = withTimeout(TIMEOUT_MS) {
            repo.toggleLike(createdPost, aliceProfile).getOrThrow()
        }
        assertTrue(likedNow)
        withTimeout(TIMEOUT_MS) {
            repo.addComment(createdPost, aliceProfile, "Midnight electric vibes!").getOrThrow()
        }
        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePostComments(aliceUid, postId).first { it.isNotEmpty() }
        }
        assertTrue(comments.any { it.text == "Midnight electric vibes!" })

        val convId = withTimeout(TIMEOUT_MS) {
            repo.getOrCreateDirectConversation(aliceProfile, aliceProfile).getOrThrow()
        }
        val conv = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeConversations(aliceUid, aliceProfile.id).first { it.any { c -> c.id == convId } }
                .first { it.id == convId }
        }
        withTimeout(TIMEOUT_MS) {
            repo.sendMessage(conv, aliceProfile, "Testing real-time VIBE chat").getOrThrow()
        }
        val msgs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(aliceUid, convId).first { it.isNotEmpty() }
        }
        assertTrue(msgs.any { it.text == "Testing real-time VIBE chat" })
    }

    @Test
    fun instagramAuthFlow_duplicateUsernameRejected_loginLogoutAndMobileRecoverySucceed() = runBlocking {
        val aliceUid = signInTestUser("alice_ig_${UUID.randomUUID()}@vibe.test")
        val aliceRepo = VibeRepository(firestore, auth)

        withTimeout(TIMEOUT_MS) {
            aliceRepo.upsertUserAccount("+91", "9876501234", true).getOrThrow()
        }

        val suffix = UUID.randomUUID().toString().replace("-", "").take(6)
        val uname1 = "ig_main_$suffix"
        val uname2 = "ig_alt_$suffix"

        val p1 = withTimeout(TIMEOUT_MS) {
            aliceRepo.createProfile(
                username = uname1,
                displayName = "Alice Main",
                bio = "Main persona",
                avatarEmoji = "⚡",
                avatarImage = "",
                cardTheme = "electric_purple",
                moodStatus = "Vibing",
                moodEmoji = "🎧",
                password = "secret123"
            ).getOrThrow()
        }

        // Duplicate username on second profile MUST fail
        val dupResult = withTimeout(TIMEOUT_MS) {
            aliceRepo.createProfile(
                username = uname1,
                displayName = "Duplicate Attempt",
                bio = "",
                avatarEmoji = "🌙",
                avatarImage = "",
                cardTheme = "cyber_cyan",
                moodStatus = "Vibing",
                moodEmoji = "🌙",
                password = "otherPassword"
            )
        }
        assertTrue("Duplicate username must be rejected", dupResult.isFailure)

        // Create second independent profile with its own password under same verified phone identity
        val p2 = withTimeout(TIMEOUT_MS) {
            aliceRepo.createProfile(
                username = uname2,
                displayName = "Alice Alt",
                bio = "Alt persona",
                avatarEmoji = "🌙",
                avatarImage = "",
                cardTheme = "cyber_cyan",
                moodStatus = "Chilling",
                moodEmoji = "✨",
                password = "altPassword99"
            ).getOrThrow()
        }

        // Log out ONLY p1 individually without disturbing p2
        withTimeout(TIMEOUT_MS) {
            aliceRepo.logoutIndividualProfile(p1).getOrThrow()
        }
        val credsAfterLogout = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMyProfileCredentials(aliceUid)
                .first { list -> list.firstOrNull { it.profileId == p1 }?.isLoggedOut == true }
        }
        assertEquals(false, credsAfterLogout.first { it.profileId == p2 }.isLoggedOut)

        // Wrong password fails
        val wrongLogin = withTimeout(TIMEOUT_MS) {
            aliceRepo.loginWithUsernameAndPassword(uname1, "wrongpass")
        }
        assertTrue(wrongLogin.isFailure)

        // Right password logs back in and clears isLoggedOut
        val rightLogin = withTimeout(TIMEOUT_MS) {
            aliceRepo.loginWithUsernameAndPassword(uname1, "secret123")
        }
        assertTrue(rightLogin.isSuccess)
        assertEquals(p1, rightLogin.getOrThrow().id)

        // Mobile password recovery works when mobile matches registered number
        val wrongRecovery = withTimeout(TIMEOUT_MS) {
            aliceRepo.recoverProfilePassword(uname1, "newPass456", "9999999999")
        }
        assertTrue(wrongRecovery.isFailure)

        val okRecovery = withTimeout(TIMEOUT_MS) {
            aliceRepo.recoverProfilePassword(uname1, "newPass456", "9876501234")
        }
        assertTrue(okRecovery.isSuccess)

        val loginWithNewPass = withTimeout(TIMEOUT_MS) {
            aliceRepo.loginWithUsernameAndPassword(uname1, "newPass456")
        }
        assertTrue(loginWithNewPass.isSuccess)

        // Story with Firestore story references (mediaRef & sourcePostId) succeeds
        val profile1 = rightLogin.getOrThrow()
        val storyId = withTimeout(TIMEOUT_MS) {
            aliceRepo.createStory(
                profile = profile1,
                caption = "Midnight studio drop",
                mediaData = "preset:hero",
                bgTheme = "electric_purple",
                audience = "all",
                mediaRef = "stories/sample_ref.jpg",
                sourcePostId = "post_ref_1"
            ).getOrThrow()
        }
        assertTrue(storyId.isNotEmpty())

        // Cross-user credential access denied
        signInTestUser("bob_ig_${UUID.randomUUID()}@vibe.test")
        val bobRepo = VibeRepository(firestore, auth)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeMyProfileCredentials(aliceUid).first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull("Expected PERMISSION_DENIED on cross-user profile_credentials", captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
