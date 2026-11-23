const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_EMAIL = "leonelizondo16@gmail.com";
const OP_EMAIL = "vendedor1@gmail.com";
const OTHER_EMAIL = "intruso@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 1000) {
  return new Date(Date.now() - msOffset);
}

function futureDate(days = 30) {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
}

function buildUserDoc(overrides = {}) {
  const now = pastDate(2000);
  return {
    id: "op_1",
    email: OP_EMAIL,
    username: "Carlos Ventas",
    role: "OPERATOR",
    daysGranted: 30,
    validFrom: now,
    expiresAt: futureDate(30),
    isBlocked: false,
    canAccessLegacyModules: false,
    notes: "Zona Norte",
    maxDevices: 1,
    boundDeviceIds: [],
    boundDeviceLabels: [],
    ownerUid: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write authorized_users or role_matrix", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("authorized_users").doc(OP_EMAIL).get());
  await assertFails(
    unauthDb.collection("authorized_users").doc(OP_EMAIL).set(buildUserDoc())
  );
  await assertFails(unauthDb.collection("team_config").doc("role_matrix").get());
});

test("Bootstrap superuser can create, update maxDevices, block, and delete users", async () => {
  const adminDb = testEnv
    .authenticatedContext("uid-admin", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    adminDb.collection("authorized_users").doc(OP_EMAIL).set(buildUserDoc())
  );
  await assertSucceeds(
    adminDb.collection("authorized_users").doc(OP_EMAIL).update({
      maxDevices: 3,
      isBlocked: false,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("authorized_users").doc(OP_EMAIL).delete()
  );
});

test("Active operator can read own profile and claim device slots up to maxDevices only", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("authorized_users")
      .doc(OP_EMAIL)
      .set(buildUserDoc({ maxDevices: 2 }));
  });

  const opDb = testEnv
    .authenticatedContext("uid-op", {
      email: OP_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(opDb.collection("authorized_users").doc(OP_EMAIL).get());

  // Claim device 1
  await assertSucceeds(
    opDb.collection("authorized_users").doc(OP_EMAIL).update({
      ownerUid: "uid-op",
      boundDeviceIds: ["dev-1"],
      boundDeviceLabels: ["Samsung A54"],
      updatedAt: pastDate(500),
    })
  );

  // Claim device 2 (allowed because maxDevices == 2)
  await assertSucceeds(
    opDb
      .collection("authorized_users")
      .doc(OP_EMAIL)
      .update({
        ownerUid: "uid-op",
        boundDeviceIds: ["dev-1", "dev-2"],
        boundDeviceLabels: ["Samsung A54", "Moto G84"],
        updatedAt: pastDate(400),
      })
  );

  // Claim device 3 fails (exceeds maxDevices == 2)
  await assertFails(
    opDb
      .collection("authorized_users")
      .doc(OP_EMAIL)
      .update({
        ownerUid: "uid-op",
        boundDeviceIds: ["dev-1", "dev-2", "dev-3"],
        boundDeviceLabels: ["Samsung A54", "Moto G84", "Pixel 8"],
        updatedAt: pastDate(300),
      })
  );

  // Operator cannot raise own maxDevices
  await assertFails(
    opDb.collection("authorized_users").doc(OP_EMAIL).update({
      maxDevices: 5,
      updatedAt: pastDate(200),
    })
  );
});

test("Blocked or expired operator cannot claim device slot", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("authorized_users")
      .doc("blocked@gmail.com")
      .set(buildUserDoc({ id: "op_b", email: "blocked@gmail.com", isBlocked: true }));
    await db
      .collection("authorized_users")
      .doc("expired@gmail.com")
      .set(
        buildUserDoc({
          id: "op_e",
          email: "expired@gmail.com",
          expiresAt: new Date(Date.now() - 86_400_000),
        })
      );
  });

  const blockedDb = testEnv
    .authenticatedContext("uid-b", {
      email: "blocked@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertFails(
    blockedDb.collection("authorized_users").doc("blocked@gmail.com").update({
      ownerUid: "uid-b",
      boundDeviceIds: ["dev-b"],
      boundDeviceLabels: ["Phone B"],
      updatedAt: pastDate(500),
    })
  );

  const expiredDb = testEnv
    .authenticatedContext("uid-e", {
      email: "expired@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertFails(
    expiredDb.collection("authorized_users").doc("expired@gmail.com").update({
      ownerUid: "uid-e",
      boundDeviceIds: ["dev-e"],
      boundDeviceLabels: ["Phone E"],
      updatedAt: pastDate(500),
    })
  );
});

test("Another user cannot read or overwrite operator document; role_matrix writes require admin", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("authorized_users").doc(OP_EMAIL).set(buildUserDoc());
  });

  const intruderDb = testEnv
    .authenticatedContext("uid-intruder", {
      email: OTHER_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertFails(intruderDb.collection("authorized_users").doc(OP_EMAIL).get());
  await assertFails(
    intruderDb.collection("team_config").doc("role_matrix").set({
      configType: "role_matrix",
      opPsaEnabled: true,
      updatedByUid: "uid-intruder",
      updatedAt: pastDate(500),
    })
  );
});

test("Seller workspaces isolate contacts, visits, and post_sale strictly by ownerUid", async () => {
  const seller1Db = testEnv
    .authenticatedContext("uid-seller-1", {
      email: "vendedor1@gmail.com",
      email_verified: true,
    })
    .firestore();
  const seller2Db = testEnv
    .authenticatedContext("uid-seller-2", {
      email: "vendedor2@gmail.com",
      email_verified: true,
    })
    .firestore();

  const nowDate = pastDate(1000);
  const contactDoc = {
    id: "contact-1",
    ownerUid: "uid-seller-1",
    name: "Laura Gómez",
    phone: "1155551234",
    address: "Av. Rivadavia 2200",
    neighborhood: "Caballito",
    notes: "Interesada en Senior 4",
    source: "Referido",
    callAttemptsCount: 1,
    messageAttemptsCount: 0,
    lastContactDateMs: Date.now(),
    lastContactChannel: "CALL",
    status: "INTERESTED",
    rescheduleDate: "",
    rescheduleNote: "",
    createdAtMs: Date.now(),
    updatedAt: nowDate,
  };

  const visitDoc = {
    id: "visit-1",
    ownerUid: "uid-seller-1",
    customerName: "Laura Gómez",
    phone: "1155551234",
    address: "Av. Rivadavia 2200",
    neighborhood: "Caballito",
    scheduledDate: "2026-11-21",
    scheduledTime: "16:30",
    modelInterest: "PSA Senior 4",
    status: "EN_DEMOSTRACION",
    chlorinePpm: 2.0,
    waterSource: "AGUA_CORRIENTE",
    hardnessPpm: 0,
    realLitersPerDay: 12.5,
    cartSummary: "1x PSA Senior 4",
    soldCartSummary: "",
    testPhotoPath: "",
    diagnosticTimestampMs: Date.now(),
    bottlesPerWeek: 4,
    quotedPlan: "12 cuotas",
    notes: "Demo OK",
    createdAtMs: Date.now(),
    updatedAt: nowDate,
  };

  const postSaleDoc = {
    id: "ps-1",
    ownerUid: "uid-seller-1",
    contactId: "contact-1",
    visitId: "visit-1",
    customerName: "Laura Gómez",
    customerPhone: "1155551234",
    purifierId: "senior4",
    purifierName: "PSA Senior 4",
    installedAtMs: Date.now(),
    replacementCycleMonths: 12,
    nextReplacementAtMs: Date.now() + 365 * 24 * 60 * 60 * 1000,
    lastNotifiedDayKey: "",
    status: "ACTIVO",
    notes: "Instalado en cocina",
    createdAtMs: Date.now(),
    updatedAt: nowDate,
  };

  // Owner succeeds
  await assertSucceeds(
    seller1Db.collection("seller_workspaces").doc("uid-seller-1").collection("contacts").doc("contact-1").set(contactDoc)
  );
  await assertSucceeds(
    seller1Db.collection("seller_workspaces").doc("uid-seller-1").collection("visits").doc("visit-1").set(visitDoc)
  );
  await assertSucceeds(
    seller1Db.collection("seller_workspaces").doc("uid-seller-1").collection("post_sale").doc("ps-1").set(postSaleDoc)
  );
  await assertSucceeds(
    seller1Db.collection("seller_workspaces").doc("uid-seller-1").collection("contacts").get()
  );
  await assertSucceeds(
    seller1Db.collection("seller_workspaces").doc("uid-seller-1").collection("visits").get()
  );
  await assertSucceeds(
    seller1Db.collection("seller_workspaces").doc("uid-seller-1").collection("post_sale").get()
  );

  // Cross-seller reads and writes are denied
  await assertFails(
    seller2Db.collection("seller_workspaces").doc("uid-seller-1").collection("contacts").doc("contact-1").get()
  );
  await assertFails(
    seller2Db.collection("seller_workspaces").doc("uid-seller-1").collection("visits").get()
  );
  await assertFails(
    seller2Db.collection("seller_workspaces").doc("uid-seller-1").collection("post_sale").doc("ps-hack").set(postSaleDoc)
  );
});

test("Seller writes own seller_stats; only bootstrap admin can list all seller_stats", async () => {
  const seller1Db = testEnv
    .authenticatedContext("uid-seller-1", {
      email: OP_EMAIL,
      email_verified: true,
    })
    .firestore();
  const seller2Db = testEnv
    .authenticatedContext("uid-seller-2", {
      email: "vendedor2@gmail.com",
      email_verified: true,
    })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext("uid-admin", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const statsDoc = {
    ownerUid: "uid-seller-1",
    sellerName: "Carlos Ventas",
    sellerEmail: OP_EMAIL,
    totalContacts: 14,
    totalVisits: 7,
    completedDemos: 5,
    closedSales: 3,
    pendingReplacements: 1,
    updatedAt: pastDate(500),
  };

  await assertSucceeds(seller1Db.collection("seller_stats").doc("uid-seller-1").set(statsDoc));
  await assertFails(seller2Db.collection("seller_stats").doc("uid-seller-1").set(statsDoc));
  await assertFails(seller2Db.collection("seller_stats").get());
  await assertSucceeds(adminDb.collection("seller_stats").get());
});

test("app_version allows superuser writes with valid versionCode and signed-in reads only", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const sellerDb = testEnv
    .authenticatedContext("uid-seller-1", {
      email: OP_EMAIL,
      email_verified: true,
    })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext("uid-admin", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const validAppVersion = {
    configType: "app_version",
    latestVersionCode: 3,
    latestVersionName: "1.2",
    apkDownloadUrl: "https://github.com/Elizondo17/Descargar-Como-On/releases/latest/download/ComoOn-PSA.apk",
    releaseNotes: "Nuevas mejoras y precios actualizados.",
    forceUpdate: false,
    updatedByUid: "uid-admin",
    updatedAt: pastDate(500),
  };

  // Non-admin cannot publish or tamper with app_version
  await assertFails(
    sellerDb.collection("team_config").doc("app_version").set({
      ...validAppVersion,
      updatedByUid: "uid-seller-1",
    })
  );

  // Admin cannot publish out-of-range versionCode
  await assertFails(
    adminDb.collection("team_config").doc("app_version").set({
      ...validAppVersion,
      latestVersionCode: 0,
    })
  );

  // Admin can publish valid app_version (optional first, then global mandatory)
  await assertSucceeds(adminDb.collection("team_config").doc("app_version").set(validAppVersion));
  await assertSucceeds(
    adminDb.collection("team_config").doc("app_version").update({
      forceUpdate: true,
      updatedAt: pastDate(200),
    })
  );

  // Signed-in user can read app_version; unauthenticated user cannot
  await assertSucceeds(sellerDb.collection("team_config").doc("app_version").get());
  await assertFails(unauthDb.collection("team_config").doc("app_version").get());
});

test("Superuser can enforce per-seller updateRequired lock; seller can only clear own updateRequired on upgrade", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("authorized_users").doc(OP_EMAIL).set(buildUserDoc());
  });

  const adminDb = testEnv
    .authenticatedContext("uid-admin", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const sellerDb = testEnv
    .authenticatedContext("uid-seller-1", {
      email: OP_EMAIL,
      email_verified: true,
    })
    .firestore();

  // Admin sets per-user updateRequired lock + requiredVersionCode
  await assertSucceeds(
    adminDb.collection("authorized_users").doc(OP_EMAIL).update({
      updateRequired: true,
      requiredVersionCode: 5,
      updateLockReason: "Actualiza a v1.4 para continuar.",
      updatedAt: pastDate(400),
    })
  );

  // Seller cannot modify requiredVersionCode or isBlocked
  await assertFails(
    sellerDb.collection("authorized_users").doc(OP_EMAIL).update({
      ownerUid: "uid-seller-1",
      requiredVersionCode: 1,
      updatedAt: pastDate(300),
    })
  );

  // Active non-blocked seller can clear own updateRequired = false when opening upgraded APK
  await assertSucceeds(
    sellerDb.collection("authorized_users").doc(OP_EMAIL).update({
      ownerUid: "uid-seller-1",
      updateRequired: false,
      updateLockReason: "",
      updatedAt: pastDate(200),
    })
  );

  // Seller CANNOT self-lock by setting updateRequired = true
  await assertFails(
    sellerDb.collection("authorized_users").doc(OP_EMAIL).update({
      updateRequired: true,
      updatedAt: pastDate(100),
    })
  );
});

test("AppErrors enforces authenticated owner create and superuser-only listing, resolution, and deletion", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const seller1Db = testEnv
    .authenticatedContext("uid-seller-1", {
      email: OP_EMAIL,
      email_verified: true,
    })
    .firestore();
  const seller2Db = testEnv
    .authenticatedContext("uid-seller-2", {
      email: "vendedor2@gmail.com",
      email_verified: true,
    })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext("uid-admin", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const errDoc = {
    id: "err_init_001",
    ownerUid: "uid-seller-1",
    reporterEmail: OP_EMAIL,
    reporterName: "Carlos Ventas",
    stage: "ON_CREATE",
    errorMessage: "Default FirebaseApp is not initialized",
    stackTraceExcerpt: "java.lang.IllegalStateException: Default FirebaseApp is not initialized",
    logcatExcerpt: "E/FirebaseApp: Failed to initialize",
    deviceModel: "Samsung SM-A546E",
    androidSdk: 34,
    appVersionName: "1.2",
    appVersionCode: 3,
    status: "OPEN",
    occurredAtMs: Date.now() - 1000,
    createdAt: pastDate(500),
    updatedAt: pastDate(500),
  };

  // Unauthenticated fails
  await assertFails(unauthDb.collection("AppErrors").doc("err_init_001").set(errDoc));

  // Forged ownerUid fails
  await assertFails(
    seller2Db.collection("AppErrors").doc("err_init_001").set(errDoc)
  );

  // Pre-resolved status on create fails
  await assertFails(
    seller1Db.collection("AppErrors").doc("err_init_002").set({
      ...errDoc,
      id: "err_init_002",
      status: "RESOLVED",
    })
  );

  // Valid seller error creation succeeds
  await assertSucceeds(
    seller1Db.collection("AppErrors").doc("err_init_001").set(errDoc)
  );

  // Sellers cannot list AppErrors or read/resolve/delete another user's error
  await assertFails(seller1Db.collection("AppErrors").get());
  await assertFails(seller2Db.collection("AppErrors").doc("err_init_001").get());
  await assertFails(
    seller1Db.collection("AppErrors").doc("err_init_001").update({
      status: "RESOLVED",
      updatedAt: pastDate(100),
    })
  );
  await assertFails(seller1Db.collection("AppErrors").doc("err_init_001").delete());

  // Bootstrap Admin can list, mark RESOLVED, and delete
  await assertSucceeds(adminDb.collection("AppErrors").get());
  await assertSucceeds(
    adminDb.collection("AppErrors").doc("err_init_001").update({
      status: "RESOLVED",
      updatedAt: pastDate(100),
    })
  );
  await assertSucceeds(adminDb.collection("AppErrors").doc("err_init_001").delete());
});

test("Superuser manages access_tokens; authenticated user can redeem valid AVAILABLE token via WriteBatch", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const seller1Db = testEnv
    .authenticatedContext("uid-seller-1", {
      email: OP_EMAIL,
      email_verified: true,
    })
    .firestore();
  const seller2Db = testEnv
    .authenticatedContext("uid-seller-2", {
      email: "vendedor2@gmail.com",
      email_verified: true,
    })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext("uid-admin", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const tokenCode = "PSA-30D-AB12CD34";
  const tokenDoc = {
    tokenCode,
    assignedUsername: "Carlos Ventas",
    targetEmail: OP_EMAIL,
    daysGranted: 30,
    maxDevices: 2,
    notes: "Alta comercial",
    status: "AVAILABLE",
    createdByUid: "uid-admin",
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  };

  // Non-admin and unauthenticated users cannot create or list tokens
  await assertFails(unauthDb.collection("access_tokens").doc(tokenCode).set(tokenDoc));
  await assertFails(seller1Db.collection("access_tokens").doc(tokenCode).set(tokenDoc));
  await assertFails(seller1Db.collection("access_tokens").get());

  // Admin creates and lists token
  await assertSucceeds(adminDb.collection("access_tokens").doc(tokenCode).set(tokenDoc));
  await assertSucceeds(adminDb.collection("access_tokens").get());

  // Wrong user (targetEmail mismatch) cannot redeem token even in WriteBatch
  const wrongUserExpires = new Date(Date.now() + 30 * 86400 * 1000);
  const wrongBatch = seller2Db.batch();
  wrongBatch.set(seller2Db.collection("authorized_users").doc("vendedor2@gmail.com"), {
    id: "op_tok_wrong",
    email: "vendedor2@gmail.com",
    username: "Otro",
    role: "OPERATOR",
    daysGranted: 30,
    validFrom: pastDate(200),
    expiresAt: wrongUserExpires,
    isBlocked: false,
    canAccessLegacyModules: false,
    notes: "Alta comercial",
    maxDevices: 2,
    boundDeviceIds: ["dev-2"],
    boundDeviceLabels: ["Pixel 8"],
    ownerUid: "uid-seller-2",
    accessToken: tokenCode,
    createdAt: pastDate(200),
    updatedAt: pastDate(200),
  });
  wrongBatch.update(seller2Db.collection("access_tokens").doc(tokenCode), {
    status: "REDEEMED",
    redeemedByEmail: "vendedor2@gmail.com",
    redeemedByUid: "uid-seller-2",
    redeemedAt: pastDate(200),
    expiresAt: wrongUserExpires,
    updatedAt: pastDate(200),
  });
  await assertFails(wrongBatch.commit());

  // Seller cannot redeem token with tampered role=SUPERUSER or tampered daysGranted=3650
  const tamperedBatch = seller1Db.batch();
  tamperedBatch.set(seller1Db.collection("authorized_users").doc(OP_EMAIL), {
    id: "op_tok_tamper",
    email: OP_EMAIL,
    username: "Carlos Ventas",
    role: "SUPERUSER",
    daysGranted: 30,
    validFrom: pastDate(200),
    expiresAt: wrongUserExpires,
    isBlocked: false,
    canAccessLegacyModules: false,
    notes: "",
    maxDevices: 2,
    boundDeviceIds: ["dev-1"],
    boundDeviceLabels: ["Pixel 7"],
    ownerUid: "uid-seller-1",
    accessToken: tokenCode,
    createdAt: pastDate(200),
    updatedAt: pastDate(200),
  });
  tamperedBatch.update(seller1Db.collection("access_tokens").doc(tokenCode), {
    status: "REDEEMED",
    redeemedByEmail: OP_EMAIL,
    redeemedByUid: "uid-seller-1",
    redeemedAt: pastDate(200),
    expiresAt: wrongUserExpires,
    updatedAt: pastDate(200),
  });
  await assertFails(tamperedBatch.commit());

  // Target seller atomically redeems AVAILABLE token via WriteBatch
  const validExpires = new Date(Date.now() + 29 * 86400 * 1000);
  const validBatch = seller1Db.batch();
  validBatch.set(seller1Db.collection("authorized_users").doc(OP_EMAIL), {
    id: "op_tok_ok",
    email: OP_EMAIL,
    username: "Carlos Ventas",
    role: "OPERATOR",
    daysGranted: 30,
    validFrom: pastDate(200),
    expiresAt: validExpires,
    isBlocked: false,
    canAccessLegacyModules: false,
    notes: "Alta comercial",
    maxDevices: 2,
    boundDeviceIds: ["dev-1"],
    boundDeviceLabels: ["Pixel 7"],
    ownerUid: "uid-seller-1",
    accessToken: tokenCode,
    createdAt: pastDate(200),
    updatedAt: pastDate(200),
  });
  validBatch.update(seller1Db.collection("access_tokens").doc(tokenCode), {
    status: "REDEEMED",
    redeemedByEmail: OP_EMAIL,
    redeemedByUid: "uid-seller-1",
    redeemedAt: pastDate(200),
    expiresAt: validExpires,
    updatedAt: pastDate(200),
  });
  await assertSucceeds(validBatch.commit());

  // Redeemed token cannot be redeemed a second time
  const replayBatch = seller1Db.batch();
  replayBatch.update(seller1Db.collection("access_tokens").doc(tokenCode), {
    status: "REDEEMED",
    redeemedByEmail: OP_EMAIL,
    redeemedByUid: "uid-seller-1",
    redeemedAt: pastDate(100),
    expiresAt: validExpires,
    updatedAt: pastDate(100),
  });
  await assertFails(replayBatch.commit());

  // Admin can create an open token, revoke it, and delete it; seller cannot revoke or delete
  const openTokenCode = "PSA-15D-ZZ99YY88";
  await assertSucceeds(
    adminDb.collection("access_tokens").doc(openTokenCode).set({
      tokenCode: openTokenCode,
      assignedUsername: "Nuevo Vendedor",
      targetEmail: "",
      daysGranted: 15,
      maxDevices: 1,
      notes: "",
      status: "AVAILABLE",
      createdByUid: "uid-admin",
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertFails(seller1Db.collection("access_tokens").doc(openTokenCode).delete());
  await assertSucceeds(
    adminDb.collection("access_tokens").doc(openTokenCode).update({
      status: "REVOKED",
      updatedAt: pastDate(100),
    })
  );
  await assertSucceeds(adminDb.collection("access_tokens").doc(openTokenCode).delete());
});

