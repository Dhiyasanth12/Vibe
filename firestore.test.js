const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);
const pastDate = () => new Date(Date.now() - 2000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated caller cannot read users, profiles, posts, or conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("profiles").where("isDeactivated", "==", false).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("conversations").where("participantUids", "array-contains", ALICE_UID).get());
});

test("PII Isolation: Bob cannot read or write Alice's private user account doc", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneCountryCode: "+91",
      phoneNumber: "9876543210",
      phoneVerified: true,
      activeProfileId: "prof_alice_1",
      profileCount: 1,
      isDarkMode: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: BOB_UID,
      phoneCountryCode: "+91",
      phoneNumber: "9999999999",
      phoneVerified: false,
      profileCount: 1,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Multi-profile: Alice can create profiles under one UID; Bob cannot spoof or delete Alice's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("profiles").doc("prof_alice_1").set({
      ownerUid: ALICE_UID,
      username: "alice_vibes",
      displayName: "Alice",
      bio: "Midnight creator",
      cardTheme: "electric_purple",
      anonymousDiscovery: false,
      isDeactivated: false,
      isOnline: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      streakDays: 1,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("profiles").doc("prof_spoof").set({
      ownerUid: ALICE_UID,
      username: "spoof_alice",
      displayName: "Spoof",
      cardTheme: "electric_purple",
      anonymousDiscovery: false,
      isDeactivated: false,
      isOnline: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      streakDays: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("profiles").doc("prof_alice_1").delete());
  await assertFails(
    aliceDb.collection("profiles").doc("prof_alice_1").update({
      isAdmin: true,
      updatedAt: pastDate(),
    })
  );
});

test("Conversations & Messages: Charlie cannot read or inject into Alice-Bob conversation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      createdByUid: ALICE_UID,
      participantUids: [ALICE_UID, BOB_UID],
      participantProfileIds: ["prof_alice_1", "prof_bob_1"],
      isGroup: false,
      chatTheme: "midnight_purple",
      disappearingSeconds: 0,
      lastMessage: "Hey!",
      streakCount: 1,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
      conversationId: "conv_1",
      participantUids: [ALICE_UID, BOB_UID],
      senderUid: ALICE_UID,
      senderProfileId: "prof_alice_1",
      senderName: "Alice",
      text: "Hello VIBE!",
      mediaType: "text",
      expiresAtMillis: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
  await assertFails(
    charlieDb.collection("conversations").doc("conv_1").collection("messages").get()
  );
  await assertFails(
    aliceDb.collection("conversations").get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("conversations").doc("dm_nonexistent_check").get());
  await assertSucceeds(
    bobDb.collection("conversations").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages").where("participantUids", "array-contains", BOB_UID).get()
  );
});

test("Posts, Likes & Comments: aligned queries succeed and cross-user delete fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      ownerUid: ALICE_UID,
      profileId: "prof_alice_1",
      authorUsername: "alice_vibes",
      authorDisplayName: "Alice",
      caption: "Neon night #vibe",
      hashtags: ["vibe"],
      mediaType: "text",
      visibility: "public",
      isReel: false,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("posts").where("visibility", "in", ["public", "close_friends"]).get());
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      ownerUid: BOB_UID,
      profileId: "prof_bob_1",
      authorUsername: "bob_beats",
      text: "Fire!",
      createdAt: pastDate(),
    })
  );
});

test("Notifications: strictly scoped to targetOwnerUid", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("notifications").doc("n_1").set({
      actorUid: BOB_UID,
      actorProfileId: "prof_bob_1",
      actorUsername: "bob_beats",
      targetOwnerUid: ALICE_UID,
      targetProfileId: "prof_alice_1",
      type: "like",
      message: "bob_beats liked your post",
      isRead: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertFails(bobDb.collection("notifications").doc("n_1").get());
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("notifications").where("targetOwnerUid", "==", ALICE_UID).get()
  );
});

test("Profile Credentials: strict owner-only access, Bob cannot read or hijack", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("profile_credentials").doc("prof_alice_1").set({
      ownerUid: ALICE_UID,
      profileId: "prof_alice_1",
      username: "alice_vibes",
      passwordHash: "dGVzdGhhc2g=",
      passwordSalt: "dGVzdHNhbHQ=",
      recoveryPhoneE164: "+919876543210",
      isLoggedOut: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("profile_credentials").where("ownerUid", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("profile_credentials").doc("prof_alice_1").update({
      isLoggedOut: true,
      updatedAt: pastDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("profile_credentials").doc("prof_alice_1").get());
  await assertFails(
    bobDb.collection("profile_credentials").where("ownerUid", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("profile_credentials").doc("prof_alice_1").update({
      isLoggedOut: false,
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("profile_credentials").doc("prof_alice_1").delete());

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("profile_credentials").doc("prof_alice_1").get());
});

test("Username Registry: atomic duplicate prevention, list denied, update denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("usernames").doc("alice_unique").get());
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_unique").set({
      username: "alice_unique",
      profileId: "prof_alice_1",
      ownerUid: ALICE_UID,
      createdAt: pastDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("usernames").doc("alice_unique").get());
  await assertFails(bobDb.collection("usernames").get());
  await assertFails(
    bobDb.collection("usernames").doc("alice_unique").set({
      username: "alice_unique",
      profileId: "prof_bob_1",
      ownerUid: BOB_UID,
      createdAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("usernames").doc("alice_unique").delete());
  await assertFails(
    aliceDb.collection("usernames").doc("alice_unique").update({
      profileId: "prof_alice_2",
    })
  );
});

