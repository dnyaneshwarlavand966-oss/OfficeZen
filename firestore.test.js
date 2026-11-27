const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_1";
const MANAGER_UID = "manager_2";
const EMP_UID = "emp_3";
const OUTSIDER_UID = "outsider_4";
const OFFICE_ID = "office_zen_1";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const nowTs = () => new Date();

async function seedOfficeWithMembers() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = nowTs();
    await db.collection("offices").doc(OFFICE_ID).set({
      ownerId: OWNER_UID,
      name: "Zen Traders",
      inviteCode: "ZEN482",
      officeLat: 19.076,
      officeLng: 72.8777,
      geofenceRadiusMeters: 150,
      strictGeofenceBlock: false,
      shiftStartMinutes: 570,
      shiftEndMinutes: 1110,
      lateGraceMinutes: 15,
      lateMarksPerHalfDay: 3,
      workingDaysPerMonth: 26,
      monthEndReportDay: 30,
      ownerReportEmail: "owner@zentraders.in",
      ownerWhatsappPhone: "+919876543210",
      teaDefaultPriceInr: 12,
      coffeeDefaultPriceInr: 20,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("offices").doc(OFFICE_ID).collection("members").doc(OWNER_UID).set({
      userId: OWNER_UID,
      officeId: OFFICE_ID,
      ownerId: OWNER_UID,
      displayName: "Rajesh Owner",
      designation: "Director",
      role: "owner",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("offices").doc(OFFICE_ID).collection("members").doc(MANAGER_UID).set({
      userId: MANAGER_UID,
      officeId: OFFICE_ID,
      ownerId: OWNER_UID,
      displayName: "Meera Manager",
      designation: "Office Manager",
      role: "manager",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("offices").doc(OFFICE_ID).collection("members").doc(EMP_UID).set({
      userId: EMP_UID,
      officeId: OFFICE_ID,
      ownerId: OWNER_UID,
      displayName: "Amit Staff",
      designation: "Executive",
      role: "employee",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("offices").doc(OFFICE_ID).collection("payroll_profiles").doc(EMP_UID).set({
      userId: EMP_UID,
      officeId: OFFICE_ID,
      ownerId: OWNER_UID,
      monthlySalaryInr: 25000,
      overtimeHourlyRateInr: 150,
      phone: "9876500001",
      email: "amit@zentraders.in",
      createdAt: ts,
      updatedAt: ts,
    });
  });
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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated caller is denied on users, offices, and subcollections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(OWNER_UID).get());
  await assertFails(unauth.collection("offices").doc(OFFICE_ID).get());
  await assertFails(
    unauth.collection("offices").doc(OFFICE_ID).collection("documents").get()
  );
});

test("User profile isolation and shadow-field rejection", async () => {
  const empDb = testEnv.authenticatedContext(EMP_UID).firestore();
  const ts = nowTs();
  await assertSucceeds(
    empDb.collection("users").doc(EMP_UID).set({
      userId: EMP_UID,
      displayName: "Amit",
      email: "amit@example.com",
      activeOfficeId: "",
      preferredLanguage: "hi",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Ghost field fails
  await assertFails(
    empDb.collection("users").doc(EMP_UID).set({
      userId: EMP_UID,
      displayName: "Amit",
      email: "amit@example.com",
      activeOfficeId: "",
      preferredLanguage: "hi",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Cross-user read fails
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertFails(ownerDb.collection("users").doc(EMP_UID).get());
});

test("Employee cannot self-escalate role or edit salary", async () => {
  await seedOfficeWithMembers();
  const empDb = testEnv.authenticatedContext(EMP_UID).firestore();
  const ts = nowTs();

  // Employee cannot promote themselves to owner/manager
  await assertFails(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("members")
      .doc(EMP_UID)
      .update({ role: "owner", updatedAt: ts })
  );

  // Employee cannot update their own monthly salary
  await assertFails(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("payroll_profiles")
      .doc(EMP_UID)
      .update({ monthlySalaryInr: 999999, updatedAt: ts })
  );

  // Owner CAN update employee role and monthly salary
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(
    ownerDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("payroll_profiles")
      .doc(EMP_UID)
      .update({ monthlySalaryInr: 28000, updatedAt: ts })
  );
});

test("Attendance rejects mock locations and restricts employee list to self", async () => {
  await seedOfficeWithMembers();
  const empDb = testEnv.authenticatedContext(EMP_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(MANAGER_UID).firestore();
  const ts = nowTs();

  const validAttendance = {
    userId: EMP_UID,
    officeId: OFFICE_ID,
    userName: "Amit Staff",
    dateKey: "2026-11-16",
    checkInEpochMs: 1763280000000,
    checkOutEpochMs: 0,
    checkInLat: 19.0761,
    checkInLng: 72.8778,
    distanceMeters: 14.5,
    isOutOfRange: false,
    isMockLocation: false,
    status: "on_time",
    lateMinutes: 0,
    overtimeMinutes: 0,
    selfieBase64: "abc",
    note: "",
    createdAt: ts,
    updatedAt: ts,
  };

  // Mock location is rejected by rules
  await assertFails(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("attendance")
      .doc("att_mock")
      .set({ ...validAttendance, isMockLocation: true })
  );

  // Valid check-in succeeds
  await assertSucceeds(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("attendance")
      .doc("att_1")
      .set(validAttendance)
  );

  // Employee listing ALL attendance fails; filtered to self succeeds
  await assertFails(
    empDb.collection("offices").doc(OFFICE_ID).collection("attendance").get()
  );
  await assertSucceeds(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("attendance")
      .where("userId", "==", EMP_UID)
      .get()
  );

  // Manager CAN list all attendance
  await assertSucceeds(
    mgrDb.collection("offices").doc(OFFICE_ID).collection("attendance").get()
  );

  // Employee can update check-out fields matching WorkManager upload payload
  await assertSucceeds(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("attendance")
      .doc("att_1")
      .update({
        checkOutEpochMs: 1763312400000,
        overtimeMinutes: 45,
        note: "Shift complete",
        updatedAt: ts,
      })
  );

  // Shadow fields (syncStatus / id) are rejected on create
  await assertFails(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("attendance")
      .doc("att_shadow")
      .set({ ...validAttendance, syncStatus: "pending" })
  );
});

test("Leave approval: employee cannot approve own leave; manager can approve once", async () => {
  await seedOfficeWithMembers();
  const empDb = testEnv.authenticatedContext(EMP_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(MANAGER_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("leaves")
      .doc("leave_1")
      .set({
        userId: EMP_UID,
        officeId: OFFICE_ID,
        userName: "Amit Staff",
        leaveType: "casual",
        startDateKey: "2026-11-20",
        endDateKey: "2026-11-20",
        daysCount: 1,
        reason: "Family function",
        status: "pending",
        reviewedBy: "",
        reviewNote: "",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Employee cannot approve own leave
  await assertFails(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("leaves")
      .doc("leave_1")
      .update({ status: "approved", reviewedBy: EMP_UID, updatedAt: ts })
  );

  // Manager approves leave
  await assertSucceeds(
    mgrDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("leaves")
      .doc("leave_1")
      .update({
        status: "approved",
        reviewedBy: MANAGER_UID,
        reviewNote: "Approved",
        updatedAt: ts,
      })
  );

  // Terminal state lock prevents re-modification
  await assertFails(
    mgrDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("leaves")
      .doc("leave_1")
      .update({
        status: "rejected",
        reviewedBy: MANAGER_UID,
        updatedAt: ts,
      })
  );
});

test("Audit log is append-only and outsiders are blocked from all collections", async () => {
  await seedOfficeWithMembers();
  const empDb = testEnv.authenticatedContext(EMP_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    empDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("audit_logs")
      .doc("log_1")
      .set({
        officeId: OFFICE_ID,
        actorUid: EMP_UID,
        actorName: "Amit Staff",
        actorRole: "employee",
        actionType: "CHECK_IN",
        summary: "Checked in on time",
        createdAt: ts,
      })
  );

  // Employee cannot read audit logs; owner can
  await assertFails(
    empDb.collection("offices").doc(OFFICE_ID).collection("audit_logs").get()
  );
  await assertSucceeds(
    ownerDb.collection("offices").doc(OFFICE_ID).collection("audit_logs").get()
  );

  // Even owner cannot update or delete audit logs
  await assertFails(
    ownerDb
      .collection("offices")
      .doc(OFFICE_ID)
      .collection("audit_logs")
      .doc("log_1")
      .delete()
  );

  // Outsider cannot read documents or tea logs
  await assertFails(
    outsiderDb.collection("offices").doc(OFFICE_ID).collection("documents").get()
  );
  await assertFails(
    outsiderDb.collection("offices").doc(OFFICE_ID).collection("tea_logs").get()
  );
});
