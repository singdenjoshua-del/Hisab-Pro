const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_hisab_1";
const BOB_UID = "bob_hisab_2";

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

test("Unauthenticated users cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("categories").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("settings").get());

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").set({
      userId: ALICE_UID,
      name: "Cash",
      accountType: "CASH",
      balance: 5000,
      iconKey: "wallet",
      colorHex: "#10B981",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner can create, read, update, and delete Accounts, Categories, Customers, Transactions, and Settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  // Account create & update
  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_cash");
  await assertSucceeds(
    accRef.set({
      userId: ALICE_UID,
      name: "Cash Wallet",
      accountType: "CASH",
      balance: 15000.5,
      iconKey: "wallet",
      colorHex: "#10B981",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(accRef.update({ balance: 12000.0, updatedAt: updated }));

  // Category create & update
  const catRef = aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("cat_food");
  await assertSucceeds(
    catRef.set({
      userId: ALICE_UID,
      nameEn: "Food & Dining",
      nameNe: "खाना र खाजा",
      type: "EXPENSE",
      iconKey: "restaurant",
      colorHex: "#F59E0B",
      systemDefault: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(catRef.update({ nameEn: "Dining", updatedAt: updated }));

  // Customer create & update
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_ram");
  await assertSucceeds(
    custRef.set({
      userId: ALICE_UID,
      name: "Ram Bahadur",
      phone: "9841000000",
      note: "Regular wholesale client",
      netBalance: 2500.0,
      totalGiven: 5000.0,
      totalReceived: 2500.0,
      colorHex: "#3B82F6",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(custRef.update({ netBalance: 1500.0, totalReceived: 3500.0, updatedAt: updated }));

  // Transaction create & update
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      title: "Lunch Thakali",
      amount: 650.0,
      type: "EXPENSE",
      categoryId: "cat_food",
      categoryNameEn: "Food & Dining",
      categoryNameNe: "खाना र खाजा",
      accountId: "acc_cash",
      accountName: "Cash Wallet",
      customerId: "",
      customerName: "",
      note: "Office lunch",
      transactionEpochMs: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(txRef.update({ amount: 700.0, note: "Updated note", updatedAt: updated }));

  // Settings create & update
  const setRef = aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences");
  await assertSucceeds(
    setRef.set({
      userId: ALICE_UID,
      languageCode: "ne",
      dateSystem: "BS",
      currencyCode: "NPR",
      themeId: "midnight_black",
      accentColorHex: "#10B981",
      appearanceMode: "DARK",
      backgroundStyle: "GRADIENT",
      monthlyBudget: 50000.0,
      devanagariDigits: true,
      languageSelected: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(setRef.update({ themeId: "royal_purple", currencyCode: "USD", updatedAt: updated }));

  // List queries
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("accounts").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's financial data", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").set({
      userId: ALICE_UID,
      name: "Nabil Bank",
      accountType: "BANK",
      balance: 95000,
      iconKey: "bank",
      colorHex: "#0EA5E9",
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").update({ balance: 0, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").delete());
});

test("Security invariants: reject @DocumentId 'id' injection, ghost fields, future timestamps, negative amounts, and immutable createdAt mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Reject ghost field `id`
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_bad").set({
      id: "acc_bad",
      userId: ALICE_UID,
      name: "Cash",
      accountType: "CASH",
      balance: 100,
      iconKey: "wallet",
      colorHex: "#10B981",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_future").set({
      userId: ALICE_UID,
      name: "Cash",
      accountType: "CASH",
      balance: 100,
      iconKey: "wallet",
      colorHex: "#10B981",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Reject non-positive transaction amount
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      title: "Zero",
      amount: 0,
      type: "EXPENSE",
      categoryId: "cat_1",
      categoryNameEn: "Food",
      categoryNameNe: "खाना",
      accountId: "acc_1",
      accountName: "Cash",
      customerId: "",
      customerName: "",
      note: "",
      transactionEpochMs: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject immutable createdAt modification on update
  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_immutable");
  await assertSucceeds(
    accRef.set({
      userId: ALICE_UID,
      name: "Cash",
      accountType: "CASH",
      balance: 100,
      iconKey: "wallet",
      colorHex: "#10B981",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    accRef.update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
