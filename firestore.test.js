const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      email: "a@b.com",
      userCategory: "Student",
      reduceMotion: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile create, partial update, shadow field rejection, and future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Dr. Alice",
      email: "alice@example.org",
      userCategory: "Researcher",
      reduceMotion: false,
      hiddenModules: [],
      dashboardWidgets: ["3d", "daily", "quick", "feed"],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userCategory: "Lecturer",
      reduceMotion: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost/shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp spoofing
  await assertFails(
    aliceDb.collection("users").doc("bob_999").set({
      userId: "bob_999",
      displayName: "Future",
      email: "f@b.com",
      userCategory: "Student",
      reduceMotion: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      userId: ALICE_UID,
      itemType: "cell",
      itemId: "cd4",
      title: "CD4+ T Helper",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").delete());
});

test("Project parent-gate enforces parent existence for entries and allows valid CRUD", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Entry without existing parent project must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID)
      .collection("projects").doc("missing_proj")
      .collection("entries").doc("entry_1")
      .set({
        userId: ALICE_UID,
        projectId: "missing_proj",
        entryType: "note",
        title: "Orphan note",
        content: "Body",
        status: "active",
        history: [],
        createdAt: past,
        updatedAt: past,
      })
  );

  // Create parent project first
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      name: "HIV Latency Study",
      description: "Educational workspace",
      folder: "Virology",
      status: "active",
      collaborators: ["Dr. Alice (Lead)"],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Now entry creation succeeds (no @DocumentId `id` key in map)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID)
      .collection("projects").doc("proj_1")
      .collection("entries").doc("entry_1")
      .set({
        userId: ALICE_UID,
        projectId: "proj_1",
        entryType: "note",
        title: "Initial findings",
        content: "Observed reservoir decay curves.",
        status: "active",
        tag: "HIV",
        history: ["v1 draft"],
        createdAt: past,
        updatedAt: past,
      })
  );

  // Reject @DocumentId `id` injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID)
      .collection("projects").doc("proj_1")
      .collection("entries").doc("entry_2")
      .set({
        id: "entry_2",
        userId: ALICE_UID,
        projectId: "proj_1",
        entryType: "note",
        title: "Bad ID",
        content: "Body",
        status: "active",
        history: [],
        createdAt: past,
        updatedAt: past,
      })
  );
});

test("Simulations, AcademyProgress, CustomContent, and AuditLogs match Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("simulations").doc("sim_1").set({
      userId: ALICE_UID,
      name: "Primary vs Secondary Run",
      presetKey: "vaccine",
      antigenLoad: 1.5,
      innateGain: 1.0,
      tCellGain: 1.2,
      bCellGain: 1.1,
      abClear: 1.0,
      autoTolerance: 1.0,
      rechallengeDay: 21.0,
      durationDays: 35.0,
      peakAntigen: 1.8,
      peakCytokine: 2.1,
      peakAntibody: 4.5,
      peakMemory: 0.9,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("academyProgress").doc("intro_imm").set({
      userId: ALICE_UID,
      trackId: "intro_imm",
      completedLessons: ["l1", "l2"],
      bestScore: 90,
      attempts: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customContent").doc("q_custom_1").set({
      userId: ALICE_UID,
      contentType: "quiz",
      trackId: "intro_imm",
      prompt: "Which cell class expresses CD4 and helps B-cell class switching?",
      explanation: "CD4+ T follicular helper cells provide CD40L and cytokines.",
      options: ["CD4+ T cell", "Neutrophil", "Erythrocyte", "Platelet"],
      correctIndex: 0,
      difficulty: "Undergraduate",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1").set({
      userId: ALICE_UID,
      level: "info",
      module: "Workspace",
      message: "Created HIV Latency Study",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Audit logs are immutable
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1").update({
      message: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
