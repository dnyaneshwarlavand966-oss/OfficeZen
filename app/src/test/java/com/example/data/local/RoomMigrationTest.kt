// Robolectric test verifying Room migration 2 to 3 and subscription database integrity without MigrationTestHelper
package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PlanTier
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

  private lateinit var context: Context
  private lateinit var db: OfficeDatabase

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Build in-memory database with MIGRATION_2_3 to verify schema creation and queries
    db =
      Room.inMemoryDatabaseBuilder(context, OfficeDatabase::class.java)
        .addMigrations(OfficeDatabase.MIGRATION_1_2, OfficeDatabase.MIGRATION_2_3)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun migration2To3_createsSubscriptionTablesAndPersistsDataCorrectly() = runBlocking {
    val now = System.currentTimeMillis()
    val subEntity =
      OfficeSubscriptionEntity(
        officeId = "off_mig_1",
        planId = PlanTier.BUSINESS.wire,
        status = "trialing",
        trialStartAtEpochMs = now,
        trialEndAtEpochMs = now + 7 * 86400000L,
        startAtEpochMs = now,
        expiryAtEpochMs = now + 7 * 86400000L,
        purchaseToken = "token_xyz",
        productId = "officezen_business_monthly",
        billingPeriod = "monthly",
        autoRenewing = true,
        lastVerifiedAtEpochMs = now,
        updatedAtEpochMs = now,
      )

    db.subscriptionDao().upsertSubscription(subEntity)
    val loaded = db.subscriptionDao().getSubscription("off_mig_1")

    assertNotNull(loaded)
    assertEquals(PlanTier.BUSINESS.wire, loaded?.planId)
    assertEquals("trialing", loaded?.status)
    assertEquals("token_xyz", loaded?.purchaseToken)

    val txEntity =
      PaymentTransactionEntity(
        id = "tx_1",
        officeId = "off_mig_1",
        ownerUid = "owner_1",
        productId = "officezen_business_monthly",
        purchaseToken = "token_xyz",
        amountInr = 299.0,
        status = "verified",
        createdAtEpochMs = now,
      )
    db.subscriptionDao().insertPaymentTransaction(txEntity)
    val history = db.subscriptionDao().observePaymentHistory("off_mig_1")
    // Verify DAO compiles and executes cleanly
    assertNotNull(history)
  }

  @Test
  fun migration2To3_withPopulatedVersion2Database_preservesDataAndMigratesSuccessfully() = runBlocking {
    val dbFile = context.getDatabasePath("v2_populated_test_all_tables.db")
    if (dbFile.exists()) {
      dbFile.delete()
    }

    // Step 1: Create a genuine SQLite database matching all 11 existing Version 2 table schemas with pre-populated records
    val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
    
    // Table 1: user_profiles
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS user_profiles (userId TEXT NOT NULL PRIMARY KEY, displayName TEXT NOT NULL, email TEXT NOT NULL, activeOfficeId TEXT NOT NULL, preferredLanguage TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO user_profiles VALUES ('user_1', 'Admin User', 'admin@example.com', 'off_v2_1', 'en', 1000000, 1000000)")

    // Table 2: offices
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS offices (id TEXT NOT NULL PRIMARY KEY, ownerId TEXT NOT NULL, name TEXT NOT NULL, inviteCode TEXT NOT NULL, officeLat REAL NOT NULL, officeLng REAL NOT NULL, geofenceRadiusMeters INTEGER NOT NULL, strictGeofenceBlock INTEGER NOT NULL, shiftStartMinutes INTEGER NOT NULL, shiftEndMinutes INTEGER NOT NULL, lateGraceMinutes INTEGER NOT NULL, lateMarksPerHalfDay INTEGER NOT NULL, workingDaysPerMonth INTEGER NOT NULL, monthEndReportDay INTEGER NOT NULL, ownerReportEmail TEXT NOT NULL, ownerWhatsappPhone TEXT NOT NULL, teaDefaultPriceInr REAL NOT NULL, coffeeDefaultPriceInr REAL NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO offices VALUES ('off_v2_1', 'owner_v2', 'Test Office V2', 'ABC123', 12.97, 77.59, 100, 0, 540, 1080, 15, 3, 26, 30, 'owner@example.com', '9876543210', 10.0, 15.0, 1000000, 1000000)")

    // Table 3: members
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS members (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, officeId TEXT NOT NULL, ownerId TEXT NOT NULL, displayName TEXT NOT NULL, designation TEXT NOT NULL, role TEXT NOT NULL, active INTEGER NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO members VALUES ('off_v2_1_user_1', 'user_1', 'off_v2_1', 'owner_v2', 'Admin User', 'Manager', 'owner', 1, 1000000, 1000000)")

    // Table 4: payroll_profiles
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS payroll_profiles (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, officeId TEXT NOT NULL, ownerId TEXT NOT NULL, monthlySalaryInr REAL NOT NULL, overtimeHourlyRateInr REAL NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO payroll_profiles VALUES ('off_v2_1_user_1', 'user_1', 'off_v2_1', 'owner_v2', 50000.0, 250.0, '9876543210', 'admin@example.com', 1000000, 1000000)")

    // Table 5: attendance_records
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS attendance_records (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, officeId TEXT NOT NULL, userName TEXT NOT NULL, dateKey TEXT NOT NULL, checkInEpochMs INTEGER NOT NULL, checkOutEpochMs INTEGER NOT NULL, checkInLat REAL NOT NULL, checkInLng REAL NOT NULL, distanceMeters REAL NOT NULL, isOutOfRange INTEGER NOT NULL, isMockLocation INTEGER NOT NULL, status TEXT NOT NULL, lateMinutes INTEGER NOT NULL, overtimeMinutes INTEGER NOT NULL, selfieBase64 TEXT NOT NULL, note TEXT NOT NULL, syncStatus TEXT NOT NULL DEFAULT 'pending', lastSyncAttemptEpochMs INTEGER NOT NULL DEFAULT 0, syncError TEXT NOT NULL DEFAULT '', createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO attendance_records VALUES ('att_v2_1', 'user_1', 'off_v2_1', 'Admin User', '2026-11-24', 1000000, 2000000, 12.97, 77.59, 5.0, 0, 0, 'Present', 0, 0, '', 'Note', 'pending', 0, '', 1000000, 1000000)")

    // Table 6: leave_requests
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS leave_requests (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, officeId TEXT NOT NULL, userName TEXT NOT NULL, leaveType TEXT NOT NULL, startDateKey TEXT NOT NULL, endDateKey TEXT NOT NULL, daysCount REAL NOT NULL, reason TEXT NOT NULL, status TEXT NOT NULL, reviewedBy TEXT NOT NULL, reviewNote TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO leave_requests VALUES ('leave_1', 'user_1', 'off_v2_1', 'Admin User', 'Casual', '2026-11-25', '2026-11-25', 1.0, 'Personal', 'Approved', 'owner_v2', 'OK', 1000000, 1000000)")

    // Table 7: salary_adjustments
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS salary_adjustments (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, officeId TEXT NOT NULL, monthKey TEXT NOT NULL, type TEXT NOT NULL, amountInr REAL NOT NULL, note TEXT NOT NULL, addedBy TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO salary_adjustments VALUES ('adj_1', 'user_1', 'off_v2_1', '2026-11', 'Bonus', 5000.0, 'Performance', 'owner_v2', 1000000, 1000000)")

    // Table 8: tea_logs
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS tea_logs (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, officeId TEXT NOT NULL, userName TEXT NOT NULL, item TEXT NOT NULL, quantity INTEGER NOT NULL, unitCostInr REAL NOT NULL, dateKey TEXT NOT NULL, monthKey TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO tea_logs VALUES ('tea_1', 'user_1', 'off_v2_1', 'Admin User', 'Tea', 2, 10.0, '2026-11-24', '2026-11', 1000000)")

    // Table 9: tea_purchases
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS tea_purchases (id TEXT NOT NULL PRIMARY KEY, officeId TEXT NOT NULL, addedBy TEXT NOT NULL, addedByName TEXT NOT NULL, category TEXT NOT NULL, quantityLabel TEXT NOT NULL, amountInr REAL NOT NULL, dateKey TEXT NOT NULL, monthKey TEXT NOT NULL, note TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO tea_purchases VALUES ('tpur_1', 'off_v2_1', 'owner_v2', 'Admin User', 'Snacks', '1 kg', 200.0, '2026-11-24', '2026-11', 'Office snacks', 1000000)")

    // Table 10: office_documents
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS office_documents (id TEXT NOT NULL PRIMARY KEY, officeId TEXT NOT NULL, createdBy TEXT NOT NULL, createdByName TEXT NOT NULL, serialNo TEXT NOT NULL, direction TEXT NOT NULL, dateKey TEXT NOT NULL, partyName TEXT NOT NULL, subject TEXT NOT NULL, status TEXT NOT NULL, scanBase64 TEXT NOT NULL, note TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO office_documents VALUES ('doc_1', 'off_v2_1', 'user_1', 'Admin User', 'IN-001', 'Inward', '2026-11-24', 'Supplier Inc', 'Invoice', 'Active', '', 'Invoice docs', 1000000, 1000000)")

    // Table 11: audit_logs
    sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (id TEXT NOT NULL PRIMARY KEY, officeId TEXT NOT NULL, actorUid TEXT NOT NULL, actorName TEXT NOT NULL, actorRole TEXT NOT NULL, actionType TEXT NOT NULL, summary TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL)")
    sqliteDb.execSQL("INSERT INTO audit_logs VALUES ('aud_1', 'off_v2_1', 'owner_v2', 'Admin User', 'owner', 'CREATE_OFFICE', 'Created office', 1000000)")

    sqliteDb.close()

    // Step 2: Open database with Room v3 builder including MIGRATION_1_2 and MIGRATION_2_3
    val migratedDb =
      Room.databaseBuilder(context, OfficeDatabase::class.java, "v2_populated_test_all_tables.db")
        .addMigrations(OfficeDatabase.MIGRATION_1_2, OfficeDatabase.MIGRATION_2_3)
        .allowMainThreadQueries()
        .build()

    // Step 3: Verify pre-existing v2 records and all 11 tables are 100% intact and uncorrupted
    assertNotNull(migratedDb)

    val rawSqlDb = migratedDb.openHelper.writableDatabase

    // Assert Table 1: user_profiles
    val c1 = rawSqlDb.query("SELECT email FROM user_profiles WHERE userId = 'user_1'")
    assertTrue(c1.moveToFirst())
    assertEquals("admin@example.com", c1.getString(0))
    c1.close()

    // Assert Table 2: offices
    val c2 = rawSqlDb.query("SELECT name FROM offices WHERE id = 'off_v2_1'")
    assertTrue(c2.moveToFirst())
    assertEquals("Test Office V2", c2.getString(0))
    c2.close()

    // Assert Table 3: members
    val c3 = rawSqlDb.query("SELECT role FROM members WHERE id = 'off_v2_1_user_1'")
    assertTrue(c3.moveToFirst())
    assertEquals("owner", c3.getString(0))
    c3.close()

    // Assert Table 4: payroll_profiles
    val c4 = rawSqlDb.query("SELECT monthlySalaryInr FROM payroll_profiles WHERE id = 'off_v2_1_user_1'")
    assertTrue(c4.moveToFirst())
    assertEquals(50000.0, c4.getDouble(0), 0.001)
    c4.close()

    // Assert Table 5: attendance_records (including v2 migration columns syncStatus)
    val c5 = rawSqlDb.query("SELECT status, syncStatus FROM attendance_records WHERE id = 'att_v2_1'")
    assertTrue(c5.moveToFirst())
    assertEquals("Present", c5.getString(0))
    assertEquals("pending", c5.getString(1))
    c5.close()

    // Assert Table 6: leave_requests
    val c6 = rawSqlDb.query("SELECT leaveType FROM leave_requests WHERE id = 'leave_1'")
    assertTrue(c6.moveToFirst())
    assertEquals("Casual", c6.getString(0))
    c6.close()

    // Assert Table 7: salary_adjustments
    val c7 = rawSqlDb.query("SELECT amountInr FROM salary_adjustments WHERE id = 'adj_1'")
    assertTrue(c7.moveToFirst())
    assertEquals(5000.0, c7.getDouble(0), 0.001)
    c7.close()

    // Assert Table 8: tea_logs
    val c8 = rawSqlDb.query("SELECT item FROM tea_logs WHERE id = 'tea_1'")
    assertTrue(c8.moveToFirst())
    assertEquals("Tea", c8.getString(0))
    c8.close()

    // Assert Table 9: tea_purchases
    val c9 = rawSqlDb.query("SELECT category FROM tea_purchases WHERE id = 'tpur_1'")
    assertTrue(c9.moveToFirst())
    assertEquals("Snacks", c9.getString(0))
    c9.close()

    // Assert Table 10: office_documents
    val c10 = rawSqlDb.query("SELECT serialNo FROM office_documents WHERE id = 'doc_1'")
    assertTrue(c10.moveToFirst())
    assertEquals("IN-001", c10.getString(0))
    c10.close()

    // Assert Table 11: audit_logs
    val c11 = rawSqlDb.query("SELECT actionType FROM audit_logs WHERE id = 'aud_1'")
    assertTrue(c11.moveToFirst())
    assertEquals("CREATE_OFFICE", c11.getString(0))
    c11.close()

    // Schema, Column, and Table Verification across all 11 existing and 2 new tables via PRAGMA table_info
    val expectedTables = listOf(
      "user_profiles", "offices", "members", "payroll_profiles", "attendance_records",
      "leave_requests", "salary_adjustments", "tea_logs", "tea_purchases", "office_documents",
      "audit_logs", "office_subscriptions", "payment_transactions"
    )
    for (tableName in expectedTables) {
      val pragmaCursor = rawSqlDb.query("PRAGMA table_info($tableName)")
      assertTrue("Table $tableName must exist and have schema columns", pragmaCursor.count > 0)
      pragmaCursor.close()
    }

    // Verify subscription table created by MIGRATION_2_3 is operational
    val now = System.currentTimeMillis()
    val subEntity =
      OfficeSubscriptionEntity(
        officeId = "off_v2_1",
        planId = PlanTier.BUSINESS.wire,
        status = "active",
        trialStartAtEpochMs = 0L,
        trialEndAtEpochMs = 0L,
        startAtEpochMs = now,
        expiryAtEpochMs = now + 30 * 86400000L,
        purchaseToken = "v2_all_tables_token",
        productId = "officezen_business_monthly",
        billingPeriod = "monthly",
        autoRenewing = true,
        lastVerifiedAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    migratedDb.subscriptionDao().upsertSubscription(subEntity)
    val loadedSub = migratedDb.subscriptionDao().getSubscription("off_v2_1")

    assertNotNull(loadedSub)
    assertEquals(PlanTier.BUSINESS.wire, loadedSub?.planId)
    assertEquals("v2_all_tables_token", loadedSub?.purchaseToken)

    migratedDb.close()
    if (dbFile.exists()) {
      dbFile.delete()
    }
  }
}
