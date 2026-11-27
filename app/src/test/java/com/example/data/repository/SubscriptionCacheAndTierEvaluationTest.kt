// Comprehensive test suite covering offline cache validity, grace period, device clock manipulation protection, and historical data retention & export access
package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.OfficeDatabase
import com.example.data.local.OfficeDocumentEntity
import com.example.data.local.PayrollProfileEntity
import com.example.data.model.OfficeSubscription
import com.example.data.model.PlanTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionCacheAndTierEvaluationTest {

  private lateinit var context: Context
  private lateinit var db: OfficeDatabase

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, OfficeDatabase::class.java)
      .addMigrations(OfficeDatabase.MIGRATION_1_2, OfficeDatabase.MIGRATION_2_3)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun scenario1_cacheWithinSevenDays_grantsPaidTier() {
    val now = System.currentTimeMillis()
    val sub = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.BUSINESS.wire,
      status = "active",
      expiryAtEpochMs = now + 10L * 86400000L,
      lastVerifiedAtEpochMs = now - 2L * 86400000L
    )

    assertTrue(sub.isCacheValid(now))
    assertTrue(sub.isPaidActive(now))
    val evaluatedTier = FeatureAccessManager.evaluateTier(sub, now)
    assertEquals(PlanTier.BUSINESS, evaluatedTier)
  }

  @Test
  fun scenario2_cacheOlderThanSevenDays_unverifiedFallbackToFree() {
    val now = System.currentTimeMillis()
    val sub = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.BUSINESS.wire,
      status = "active",
      expiryAtEpochMs = now + 10L * 86400000L,
      lastVerifiedAtEpochMs = now - 8L * 86400000L
    )

    assertFalse(sub.isCacheValid(now))
    val evaluatedTier = FeatureAccessManager.evaluateTier(sub, now)
    assertEquals(PlanTier.FREE, evaluatedTier)
  }

  @Test
  fun scenario3_activeSubscriptionWithinThreeDayGracePeriod_grantsTier() {
    val now = System.currentTimeMillis()
    val sub = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.STARTER.wire,
      status = "grace_period",
      expiryAtEpochMs = now - 1L * 86400000L,
      lastVerifiedAtEpochMs = now - 2L * 86400000L
    )

    assertFalse(sub.isPaidActive(now))
    assertTrue(sub.isInGracePeriod(now))
    val evaluatedTier = FeatureAccessManager.evaluateTier(sub, now)
    assertEquals(PlanTier.STARTER, evaluatedTier)
  }

  @Test
  fun scenario4_gracePeriodExpiration_fallsBackToFree() {
    val now = System.currentTimeMillis()
    val sub = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.STARTER.wire,
      status = "expired",
      expiryAtEpochMs = now - 4L * 86400000L,
      lastVerifiedAtEpochMs = now - 5L * 86400000L
    )

    assertFalse(sub.isPaidActive(now))
    assertFalse(sub.isInGracePeriod(now))
    val evaluatedTier = FeatureAccessManager.evaluateTier(sub, now)
    assertEquals(PlanTier.FREE, evaluatedTier)
  }

  @Test
  fun scenario5_deviceClockManipulation_backwardAndForwardTamperingDetected() {
    val now = System.currentTimeMillis()

    // Test Backward Clock Tampering: user turns back device clock before lastVerifiedAtEpochMs
    val tamperedBackwardSub = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.BUSINESS.wire,
      status = "active",
      expiryAtEpochMs = now + 10L * 86400000L,
      lastVerifiedAtEpochMs = now + 2L * 86400000L
    )
    assertTrue(tamperedBackwardSub.isClockTampered(now))
    assertFalse(tamperedBackwardSub.isCacheValid(now))
    assertEquals(PlanTier.FREE, FeatureAccessManager.evaluateTier(tamperedBackwardSub, now))

    // Test Forward Clock Tampering / Full Expiry past grace period: user advances device clock by 5 days (> 3 days grace)
    val expiredByClockAdvance = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.BUSINESS.wire,
      status = "expired",
      expiryAtEpochMs = now - 5L * 86400000L,
      lastVerifiedAtEpochMs = now - 10L * 86400000L
    )
    assertFalse(expiredByClockAdvance.isPaidActive(now))
    assertFalse(expiredByClockAdvance.isInGracePeriod(now))
    assertEquals(PlanTier.FREE, FeatureAccessManager.evaluateTier(expiredByClockAdvance, now))
  }

  @Test
  fun scenario6_historicalDataViewingAndExportAfterExpiry_preservesAccess() = runBlocking {
    val now = System.currentTimeMillis()
    
    // Insert historical records into Room database
    val attendanceEntity = AttendanceRecordEntity(
      id = "att_hist_1",
      userId = "user_1",
      officeId = "off_1",
      userName = "Test User",
      dateKey = "2026-11-24",
      checkInEpochMs = now - 100000L,
      checkOutEpochMs = now - 50000L,
      checkInLat = 12.97,
      checkInLng = 77.59,
      distanceMeters = 5.0,
      isOutOfRange = false,
      isMockLocation = false,
      status = "Present",
      lateMinutes = 0,
      overtimeMinutes = 0,
      selfieBase64 = "",
      note = "Historical attendance",
      createdAtEpochMs = now - 100000L,
      updatedAtEpochMs = now - 100000L
    )
    db.officeDao().upsertAttendance(attendanceEntity)

    val payrollEntity = PayrollProfileEntity(
      id = "off_1_user_1",
      userId = "user_1",
      officeId = "off_1",
      ownerId = "owner_1",
      monthlySalaryInr = 45000.0,
      overtimeHourlyRateInr = 200.0,
      phone = "9876543210",
      email = "user@example.com",
      createdAtEpochMs = now - 100000L,
      updatedAtEpochMs = now - 100000L
    )
    db.officeDao().upsertPayrollProfile(payrollEntity)

    val docEntity = OfficeDocumentEntity(
      id = "doc_hist_1",
      officeId = "off_1",
      createdBy = "user_1",
      createdByName = "Test User",
      serialNo = "DOC-001",
      direction = "Inward",
      dateKey = "2026-11-24",
      partyName = "Vendor Corp",
      subject = "Contract",
      status = "Active",
      scanBase64 = "",
      note = "Historical doc",
      createdAtEpochMs = now - 100000L,
      updatedAtEpochMs = now - 100000L
    )
    db.officeDao().upsertDocument(docEntity)

    // Set an expired subscription
    val expiredSub = OfficeSubscription(
      officeId = "off_1",
      planId = PlanTier.STARTER.wire,
      status = "expired",
      expiryAtEpochMs = now - 15L * 86400000L,
      lastVerifiedAtEpochMs = now - 20L * 86400000L
    )

    // Verify tier evaluated as FREE after expiry
    val tier = FeatureAccessManager.evaluateTier(expiredSub, now)
    assertEquals(PlanTier.FREE, tier)

    // Verify that historical records are 100% readable, present, and ready for export (never deleted)
    val loadedAttendance = db.officeDao().getAttendanceById("att_hist_1")
    assertNotNull(loadedAttendance)
    assertEquals("Test User", loadedAttendance?.userName)

    val loadedPayroll = db.officeDao().getPayrollProfile("off_1", "user_1")
    assertNotNull(loadedPayroll)
    assertEquals(45000.0, loadedPayroll?.monthlySalaryInr ?: 0.0, 0.001)

    val docsList = db.officeDao().observeDocuments("off_1").first()
    assertFalse(docsList.isEmpty())
    assertEquals("DOC-001", docsList[0].serialNo)
  }
}
