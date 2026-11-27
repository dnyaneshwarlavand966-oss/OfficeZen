// Robolectric + WorkManager TestDriver tests for connectivity-driven Room -> Firestore attendance sync
package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.OfficeDatabase
import com.example.data.local.OfficeEntity
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.Office
import com.example.data.prefs.AppPreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceSyncWorkerTest {

  private lateinit var context: Context
  private lateinit var db: OfficeDatabase
  private lateinit var preferences: AppPreferences
  private lateinit var testDriver: TestDriver

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, OfficeDatabase::class.java).allowMainThreadQueries().build()
    preferences = AppPreferences(context)

    val config =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!

    SyncWorkerDependencies.daoProvider = { db.officeDao() }
    SyncWorkerDependencies.preferencesProvider = { preferences }
  }

  @After
  fun tearDown() {
    SyncWorkerDependencies.resetDefaults()
    db.close()
  }

  private suspend fun seedPendingAttendance(
    id: String = "att_1",
    checkOutEpochMs: Long = 0L,
    syncStatus: String = AttendanceSyncStatus.PENDING,
  ) {
    db.officeDao()
      .upsertOffice(
        OfficeEntity(
          id = "office_1",
          ownerId = "local_owner",
          name = "Zen Corp",
          inviteCode = "ZEN101",
          officeLat = 19.076,
          officeLng = 72.8777,
          geofenceRadiusMeters = 150L,
          strictGeofenceBlock = false,
          shiftStartMinutes = 570L,
          shiftEndMinutes = 1110L,
          lateGraceMinutes = 15L,
          lateMarksPerHalfDay = 3L,
          workingDaysPerMonth = 26L,
          monthEndReportDay = 30L,
          ownerReportEmail = "",
          ownerWhatsappPhone = "",
          teaDefaultPriceInr = 12.0,
          coffeeDefaultPriceInr = 20.0,
          createdAtEpochMs = 1000L,
          updatedAtEpochMs = 1000L,
        )
      )
    db.officeDao()
      .upsertAttendance(
        AttendanceRecordEntity(
          id = id,
          officeId = "office_1",
          userId = "local_owner",
          userName = "Rajesh",
          dateKey = "2026-11-24",
          checkInEpochMs = 1_763_960_000_000L,
          checkOutEpochMs = checkOutEpochMs,
          checkInLat = 19.0761,
          checkInLng = 72.8778,
          distanceMeters = 12.0,
          isOutOfRange = false,
          isMockLocation = false,
          status = "on_time",
          lateMinutes = 0L,
          overtimeMinutes = if (checkOutEpochMs > 0L) 30L else 0L,
          selfieBase64 = "",
          note = "",
          syncStatus = syncStatus,
          lastSyncAttemptEpochMs = 0L,
          syncError = "",
          createdAtEpochMs = 2000L,
          updatedAtEpochMs = 2000L,
        )
      )
  }

  @Test
  fun scheduler_enqueuesConnectedConstraintWork_andUploadsPendingRoomRecordsWhenConnectivityRestored() =
    runBlocking {
      seedPendingAttendance(id = "att_offline_1", checkOutEpochMs = 1_763_992_400_000L)
      val uploaded = mutableListOf<AttendanceRecord>()

      SyncWorkerDependencies.uploaderProvider = {
        object : AttendanceRemoteUploader {
          override fun isAuthenticated(): Boolean = true
          override fun currentUserId(): String = "uid_google_1"
          override suspend fun uploadAttendanceRecord(
            record: AttendanceRecord,
            office: Office?,
          ): Result<Unit> {
            uploaded += record
            return Result.success(Unit)
          }
        }
      }

      AttendanceSyncScheduler.enqueueSyncWhenConnected(context)

      val workManager = WorkManager.getInstance(context)
      val queuedInfos =
        workManager.getWorkInfosForUniqueWork(AttendanceSyncScheduler.UNIQUE_SYNC_WORK_NAME).get()
      assertEquals(1, queuedInfos.size)
      val workId = queuedInfos.first().id
      assertEquals(WorkInfo.State.ENQUEUED, queuedInfos.first().state)
      assertEquals(NetworkType.CONNECTED, queuedInfos.first().constraints.requiredNetworkType)

      // Simulate OS network connectivity becoming available
      testDriver.setAllConstraintsMet(workId)

      val finishedInfo =
        withTimeout(5_000L) {
          workManager.getWorkInfoByIdFlow(workId).first { info -> info != null && info.state.isFinished }!!
        }
      assertEquals(WorkInfo.State.SUCCEEDED, finishedInfo.state)
      assertEquals(1, finishedInfo.outputData.getInt(AttendanceSyncWorker.KEY_UPLOADED_COUNT, -1))
      assertEquals(0, finishedInfo.outputData.getInt(AttendanceSyncWorker.KEY_FAILED_COUNT, -1))

      assertEquals(1, uploaded.size)
      assertEquals("att_offline_1", uploaded.first().id)

      val updatedEntity = db.officeDao().getAttendanceById("att_offline_1")!!
      assertEquals(AttendanceSyncStatus.SYNCED, updatedEntity.syncStatus)
      assertEquals(0, db.officeDao().observePendingAttendanceCount("office_1").first())
      assertEquals(1, db.officeDao().observeSyncedAttendanceCount("office_1").first())
      assertTrue(preferences.lastSyncMessageFlow.first().contains("Synced 1 attendance"))
    }

  @Test
  fun worker_transientIoFailure_marksRecordFailedAndRequestsRetry() = runBlocking {
    seedPendingAttendance(id = "att_io_fail")
    SyncWorkerDependencies.uploaderProvider = {
      object : AttendanceRemoteUploader {
        override fun isAuthenticated(): Boolean = true
        override fun currentUserId(): String = "uid_google_1"
        override suspend fun uploadAttendanceRecord(
          record: AttendanceRecord,
          office: Office?,
        ): Result<Unit> = Result.failure(IOException("Temporary network drop"))
      }
    }

    val worker = TestListenableWorkerBuilder<AttendanceSyncWorker>(context).build()
    val result = worker.doWork()

    assertTrue(result is ListenableWorker.Result.Retry)
    val entity = db.officeDao().getAttendanceById("att_io_fail")!!
    assertEquals(AttendanceSyncStatus.FAILED, entity.syncStatus)
    assertTrue(entity.syncError.contains("Temporary network drop"))
    assertEquals(1, db.officeDao().observePendingAttendanceCount("office_1").first())
  }

  @Test
  fun worker_permissionDeniedFailure_returnsFailureWithoutInfiniteRetry() = runBlocking {
    seedPendingAttendance(id = "att_perm_fail")
    SyncWorkerDependencies.uploaderProvider = {
      object : AttendanceRemoteUploader {
        override fun isAuthenticated(): Boolean = true
        override fun currentUserId(): String = "uid_google_1"
        override suspend fun uploadAttendanceRecord(
          record: AttendanceRecord,
          office: Office?,
        ): Result<Unit> =
          Result.failure(
            FirebaseFirestoreException(
              "Missing or insufficient permissions.",
              FirebaseFirestoreException.Code.PERMISSION_DENIED,
            )
          )
      }
    }

    val worker = TestListenableWorkerBuilder<AttendanceSyncWorker>(context).build()
    val result = worker.doWork()

    assertTrue(result is ListenableWorker.Result.Failure)
    val entity = db.officeDao().getAttendanceById("att_perm_fail")!!
    assertEquals(AttendanceSyncStatus.FAILED, entity.syncStatus)
  }

  @Test
  fun worker_unauthenticatedUser_preservesPendingRoomRecord() = runBlocking {
    seedPendingAttendance(id = "att_unauth")
    SyncWorkerDependencies.uploaderProvider = {
      object : AttendanceRemoteUploader {
        override fun isAuthenticated(): Boolean = false
        override fun currentUserId(): String? = null
        override suspend fun uploadAttendanceRecord(
          record: AttendanceRecord,
          office: Office?,
        ): Result<Unit> = error("Should not be called while signed out")
      }
    }

    val worker = TestListenableWorkerBuilder<AttendanceSyncWorker>(context).build()
    val result = worker.doWork()

    assertTrue(result is ListenableWorker.Result.Retry)
    val entity = db.officeDao().getAttendanceById("att_unauth")!!
    assertEquals(AttendanceSyncStatus.PENDING, entity.syncStatus)
    assertTrue(preferences.lastSyncMessageFlow.first().contains("Sign in with Google"))
  }

  @Test
  fun networkConnectivityObserver_detectsConnectivityChangeAndInvokesRestoreCallback() {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val shadowCm = Shadows.shadowOf(cm)

    // Start offline (no capabilities on active network)
    val activeNetwork = cm.activeNetwork
    val offlineCaps = ShadowNetworkCapabilities.newInstance()
    shadowCm.setNetworkCapabilities(activeNetwork, offlineCaps)

    var callbackCount = 0
    val observer = NetworkConnectivityObserver(context) { callbackCount++ }
    assertFalse(observer.isOnline.value)

    observer.startObserving()

    val callbacks = shadowCm.networkCallbacks
    assertFalse(callbacks.isEmpty())

    // Simulate network transitioning to validated Wi-Fi internet
    val onlineCaps = ShadowNetworkCapabilities.newInstance()
    val shadowCaps = Shadows.shadowOf(onlineCaps)
    shadowCaps.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    shadowCaps.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    shadowCaps.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    shadowCm.setNetworkCapabilities(activeNetwork, onlineCaps)

    callbacks.forEach { cb ->
      cb.onCapabilitiesChanged(activeNetwork!!, onlineCaps)
    }

    assertTrue(observer.isOnline.value)
    assertTrue(callbackCount >= 1)

    observer.stopObserving()
  }
}
