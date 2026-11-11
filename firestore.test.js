const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner";
const BOB_UID = "bob_intruder";

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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read customers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
});

test("Cross-user access: Bob cannot read Alice's customer", async () => {
  // Seed Alice's customer bypassing rules
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("customers").doc("c1").set({
      userId: ALICE_UID,
      name: "Ramesh Kumar",
      phone: "9876543210",
      totalUdhar: 500,
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("c1").get());
});

test("Owner access: Alice can create, read and update her customer", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const customerRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("c1");

  const now = new Date();
  await assertSucceeds(customerRef.set({
    userId: ALICE_UID,
    name: "Ramesh Kumar",
    phone: "9876543210",
    totalUdhar: 500,
    address: "Shop 4, Market Road",
    notes: "Regular customer",
    createdAt: now,
    updatedAt: now
  }));

  const snapshot = await assertSucceeds(customerRef.get());
  // Can read her own customer
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("customers").get());

  // Can update udhar
  await assertSucceeds(customerRef.update({
    totalUdhar: 800,
    updatedAt: new Date()
  }));
});

test("Owner access: Alice can add inventory item and store order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("item1");
  await assertSucceeds(itemRef.set({
    userId: ALICE_UID,
    name: "Aashirvaad Atta 10kg",
    hindiName: "आशीर्वाद आटा",
    category: "Atta & Flour",
    quantity: 15,
    unit: "pkt",
    costPrice: 420,
    sellingPrice: 460,
    lowStockAlert: 5,
    isAvailableForOnlineOrder: true,
    createdAt: now,
    updatedAt: now
  }));

  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order1");
  await assertSucceeds(orderRef.set({
    userId: ALICE_UID,
    customerName: "Sunita Devi",
    customerPhone: "9871122334",
    deliveryAddress: "Flat 202, Shanti Vihar",
    totalAmount: 920,
    paymentMethod: "COD",
    status: "PLACED",
    itemsSummary: "Aashirvaad Atta 10kg x 2",
    createdAt: now,
    updatedAt: now
  }));
});
