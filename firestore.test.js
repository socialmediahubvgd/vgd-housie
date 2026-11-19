const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const FOUNDER_UID = "founder_vinayak_uid";
const FOUNDER_EMAIL = "vinayakglobaldream@gmail.com";
const PLAYER_A_UID = "player_alice_uid";
const PLAYER_A_EMAIL = "alice@example.com";
const PLAYER_B_UID = "player_bob_uid";
const PLAYER_B_EMAIL = "bob@example.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function makeUserDoc(uid, email, displayName, role = "player") {
  const now = pastDate(5000);
  return {
    userId: uid,
    email,
    displayName,
    role,
    accountStatus: "active",
    diamonds: 5000,
    coins: 0,
    totalExchangesCount: 0,
    totalDiamondsExchanged: 0,
    totalCoinsDistributed: 0,
    gamesPlayed: 0,
    gamesWon: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function makePlatformConfig() {
  return {
    configId: "platform",
    exchangeEnabled: true,
    maintenanceMode: false,
    announcementBanner: "Welcome to VGD Housie!",
    smallEntryCoins: 500,
    smallPrizeDiamonds: 5000,
    mediumEntryCoins: 2000,
    mediumPrizeDiamonds: 15000,
    bigEntryCoins: 10000,
    bigPrizeDiamonds: 80000,
    vipEntryCoins: 50000,
    vipPrizeDiamonds: 400000,
    totalCoinsGeneratedByFounder: 0,
    totalDiamondsGeneratedByFounder: 0,
    updatedByEmail: FOUNDER_EMAIL,
    updatedAt: pastDate(2000),
  };
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").get());
  await assertFails(unauth.collection("users").doc(PLAYER_A_UID).get());
  await assertFails(unauth.collection("exchange_ledger").get());
  await assertFails(unauth.collection("app_config").doc("platform").get());
  await assertFails(
    unauth.collection("users").doc(PLAYER_A_UID).set(makeUserDoc(PLAYER_A_UID, PLAYER_A_EMAIL, "Alice"))
  );
});

test("Email spoofing test: unverified founder email is rejected from founder privileges", async () => {
  const spoofed = testEnv.authenticatedContext("spoof_uid", {
    email: FOUNDER_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofed.collection("users").get());
  await assertFails(spoofed.collection("exchange_ledger").get());
  await assertFails(spoofed.collection("app_config").doc("platform").set(makePlatformConfig()));
  await assertFails(
    spoofed.collection("users").doc("spoof_uid").set(makeUserDoc("spoof_uid", FOUNDER_EMAIL, "Spoofer", "founder"))
  );
});

test("Regular player can create own player account, cannot self-assign founder role or list all users", async () => {
  const aliceDb = testEnv.authenticatedContext(PLAYER_A_UID, {
    email: PLAYER_A_EMAIL,
    email_verified: true,
  }).firestore();

  // Valid player account create succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(PLAYER_A_UID).set(makeUserDoc(PLAYER_A_UID, PLAYER_A_EMAIL, "Alice", "player"))
  );

  // Privilege escalation on create fails
  await assertFails(
    aliceDb.collection("users").doc("alice_alt").set(makeUserDoc("alice_alt", PLAYER_A_EMAIL, "Alice", "founder"))
  );

  // Regular user cannot list all users or read exchange_ledger or founder_audit_logs
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("exchange_ledger").get());
  await assertFails(aliceDb.collection("founder_audit_logs").get());

  // Regular user cannot read or update Bob's account
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(PLAYER_B_UID).set(
      makeUserDoc(PLAYER_B_UID, PLAYER_B_EMAIL, "Bob", "player")
    );
  });
  await assertFails(aliceDb.collection("users").doc(PLAYER_B_UID).get());
  await assertFails(aliceDb.collection("users").doc(PLAYER_B_UID).update({ coins: 99999 }));
});

test("Regular player can perform exchange and write own subcollections + exchange_ledger entry, but cannot write platform config", async () => {
  const aliceDb = testEnv.authenticatedContext(PLAYER_A_UID, {
    email: PLAYER_A_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(PLAYER_A_UID).set(makeUserDoc(PLAYER_A_UID, PLAYER_A_EMAIL, "Alice", "player"))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      diamonds: 3500,
      coins: 900,
      totalExchangesCount: 1,
      totalDiamondsExchanged: 1500,
      totalCoinsDistributed: 900,
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(PLAYER_A_UID).collection("exchanges").doc("ex_1").set({
      userId: PLAYER_A_UID,
      diamondsExchanged: 1500,
      grossCoins: 1000,
      netCoinsReceived: 900,
      adminFeeCoins: 100,
      timestampMillis: Date.now(),
      createdAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("exchange_ledger").doc("ex_1").set({
      userId: PLAYER_A_UID,
      userEmail: PLAYER_A_EMAIL,
      userName: "Alice",
      diamondsExchanged: 1500,
      grossCoins: 1000,
      netCoinsReceived: 900,
      adminFeeCoins: 100,
      timestampMillis: Date.now(),
      createdAt: pastDate(500),
    })
  );

  // Regular player cannot overwrite platform config or un-suspend themselves
  await assertFails(aliceDb.collection("app_config").doc("platform").set(makePlatformConfig()));
  await assertFails(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      role: "founder",
      updatedAt: pastDate(200),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      accountStatus: "suspended",
      updatedAt: pastDate(200),
    })
  );
});

test("Founder (vinayakglobaldream@gmail.com) has full management access: users, coin generator, platform config, ledger, audit logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(PLAYER_A_UID).set(
      makeUserDoc(PLAYER_A_UID, PLAYER_A_EMAIL, "Alice", "player")
    );
  });

  const founderDb = testEnv.authenticatedContext(FOUNDER_UID, {
    email: FOUNDER_EMAIL,
    email_verified: true,
  }).firestore();

  // Founder creates own founder profile
  await assertSucceeds(
    founderDb.collection("users").doc(FOUNDER_UID).set(makeUserDoc(FOUNDER_UID, FOUNDER_EMAIL, "Vinayak Founder", "founder"))
  );

  // Founder lists all users and reads exchange ledger
  await assertSucceeds(founderDb.collection("users").get());
  await assertSucceeds(founderDb.collection("exchange_ledger").get());

  // Founder generates coins & diamonds for Alice and suspends/activates Alice
  await assertSucceeds(
    founderDb.collection("users").doc(PLAYER_A_UID).update({
      coins: 50000,
      diamonds: 10000,
      accountStatus: "suspended",
      updatedAt: pastDate(300),
    })
  );

  // Founder writes activity entry in Alice's wallet
  await assertSucceeds(
    founderDb.collection("users").doc(PLAYER_A_UID).collection("activities").doc("grant_1").set({
      userId: PLAYER_A_UID,
      type: "FOUNDER_GRANT",
      title: "Founder Credited 50,000 Coins",
      subtitle: "Founder Bonus",
      amountDeltaText: "+50,000 Coins",
      timestampMillis: Date.now(),
      createdAt: pastDate(300),
    })
  );

  // Founder creates & updates platform config
  await assertSucceeds(founderDb.collection("app_config").doc("platform").set(makePlatformConfig()));
  await assertSucceeds(
    founderDb.collection("app_config").doc("platform").update({
      exchangeEnabled: false,
      smallEntryCoins: 750,
      updatedAt: pastDate(100),
    })
  );

  // Founder writes audit log
  await assertSucceeds(
    founderDb.collection("founder_audit_logs").doc("log_1").set({
      actorUid: FOUNDER_UID,
      actorEmail: FOUNDER_EMAIL,
      actionType: "MINT_COINS",
      targetUserId: PLAYER_A_UID,
      targetEmail: PLAYER_A_EMAIL,
      summary: "Generated +50,000 Coins for alice@example.com",
      timestampMillis: Date.now(),
      createdAt: pastDate(100),
    })
  );

  // Suspended Alice cannot self-update her wallet or un-suspend herself
  const aliceDb = testEnv.authenticatedContext(PLAYER_A_UID, {
    email: PLAYER_A_EMAIL,
    email_verified: true,
  }).firestore();
  await assertFails(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      coins: 51000,
      updatedAt: pastDate(50),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      accountStatus: "active",
      updatedAt: pastDate(50),
    })
  );
});

test("Virtual ID directory, Profile editing, and 1v1 Chat Inbox enforce zero-trust isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(PLAYER_A_UID, {
    email: PLAYER_A_EMAIL,
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(PLAYER_B_UID, {
    email: PLAYER_B_EMAIL,
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext("player_charlie_uid", {
    email: "charlie@example.com",
    email_verified: true,
  }).firestore();

  const aliceUser = makeUserDoc(PLAYER_A_UID, PLAYER_A_EMAIL, "Alice", "player");
  aliceUser.virtualId = "VGD-ALICE1";
  const bobUser = makeUserDoc(PLAYER_B_UID, PLAYER_B_EMAIL, "Bob", "player");
  bobUser.virtualId = "VGD-BOB222";

  await assertSucceeds(aliceDb.collection("users").doc(PLAYER_A_UID).set(aliceUser));
  await assertSucceeds(bobDb.collection("users").doc(PLAYER_B_UID).set(bobUser));

  // Users can edit their own profile fields (name, nickname, gender, dob, photoBase64)
  await assertSucceeds(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      displayName: "Alice Royal",
      nickname: "QueenA",
      gender: "Female",
      dob: "15/08/1998",
      photoBase64: "ZHVtbXlwaG90bw==",
      updatedAt: pastDate(500),
    })
  );

  // Alice cannot change her immutable Virtual ID once assigned
  await assertFails(
    aliceDb.collection("users").doc(PLAYER_A_UID).update({
      virtualId: "VGD-HACKED",
      updatedAt: pastDate(300),
    })
  );

  // Virtual ID directory entry creation & exact lookup
  const now = pastDate(1000);
  await assertSucceeds(
    aliceDb.collection("virtual_ids").doc("VGD-ALICE1").set({
      virtualId: "VGD-ALICE1",
      userId: PLAYER_A_UID,
      displayName: "Alice Royal",
      nickname: "QueenA",
      gender: "Female",
      photoBase64: "ZHVtbXlwaG90bw==",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can look up Alice by exact Virtual ID
  await assertSucceeds(bobDb.collection("virtual_ids").doc("VGD-ALICE1").get());
  // Bob cannot hijack Alice's Virtual ID or blanket-list virtual_ids
  await assertFails(
    bobDb.collection("virtual_ids").doc("VGD-ALICE1").update({
      displayName: "Hijacked",
      updatedAt: pastDate(200),
    })
  );
  await assertFails(bobDb.collection("virtual_ids").get());

  // 1v1 Chat Thread & Message creation (batched so existsAfter succeeds)
  const chatId = `${PLAYER_A_UID}_${PLAYER_B_UID}`;
  const participants = [PLAYER_A_UID, PLAYER_B_UID];
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("chats").doc(chatId), {
    participantIds: participants,
    user1Id: PLAYER_A_UID,
    user1VirtualId: "VGD-ALICE1",
    user1Name: "Alice Royal",
    user1Photo: "",
    user2Id: PLAYER_B_UID,
    user2VirtualId: "VGD-BOB222",
    user2Name: "Bob",
    user2Photo: "",
    lastMessageText: "Hi Bob, ready for VIP Housie?",
    lastSenderId: PLAYER_A_UID,
    lastMessageAtMillis: Date.now(),
    createdAt: pastDate(400),
    updatedAt: pastDate(400),
  });
  batch.set(aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1"), {
    chatId,
    senderId: PLAYER_A_UID,
    senderVirtualId: "VGD-ALICE1",
    senderName: "Alice Royal",
    text: "Hi Bob, ready for VIP Housie?",
    participantIds: participants,
    timestampMillis: Date.now(),
    createdAt: pastDate(400),
  });
  await assertSucceeds(batch.commit());

  // Both Alice & Bob can query their chats and read messages
  await assertSucceeds(
    bobDb.collection("chats").where("participantIds", "array-contains", PLAYER_B_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").where("participantIds", "array-contains", PLAYER_B_UID).get()
  );

  // Outsider Charlie CANNOT read chat, read msg_1, run unfiltered message query, or spoof senderId
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_spoof").set({
      chatId,
      senderId: PLAYER_A_UID,
      senderVirtualId: "VGD-ALICE1",
      senderName: "Alice",
      text: "Spoof!",
      participantIds: [PLAYER_A_UID, "player_charlie_uid"],
      timestampMillis: Date.now(),
      createdAt: pastDate(100),
    })
  );
});
