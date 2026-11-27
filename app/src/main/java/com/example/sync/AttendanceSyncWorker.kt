// WorkManager CoroutineWorker that uploads pending Room attendance records to Firestore
package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.OfficeDao
import com.example.data.local.OfficeDatabase
import com.example.data.model.AttendanceSyncStatus
import com.example.data.prefs.AppPreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncWorkerDependencies {
  @Volatile var daoProvider: ((Context) -> OfficeDao)? = null
  @Volatile var uploaderProvider: ((Context) -> AttendanceRemoteUploader)? = null
  @Volatile var preferencesProvider: ((Context) -> AppPreferences)? = null

  fun resetDefaults() {
    daoProvider = null
    uploaderProvider = null
    preferencesProvider = null
  }
}

class AttendanceSyncWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      val dao =
        SyncWorkerDependencies.daoProvider?.invoke(applicationContext)
          ?: OfficeDatabase.getInstance(applicationContext).officeDao()
      val uploader =
        SyncWorkerDependencies.uploaderProvider?.invoke(applicationContext)
          ?: FirebaseAttendanceUploader(applicationContext)
      val preferences =
        SyncWorkerDependencies.preferencesProvider?.invoke(applicationContext)
          ?: AppPreferences(applicationContext)

      val pending = dao.getPendingAttendanceRecords()
      val now = System.currentTimeMillis()
      if (pending.isEmpty()) {
        preferences.setLastSyncResult(now, "All attendance records synced")
        return@withContext Result.success(
          workDataOf(
            KEY_UPLOADED_COUNT to 0,
            KEY_FAILED_COUNT to 0,
          )
        )
      }

      if (!uploader.isAuthenticated()) {
        val msg = "${pending.size} pending · Sign in with Google to upload to Firestore"
        preferences.setLastSyncResult(now, msg)
        Log.w(TAG, "Skipping attendance sync ($msg)")
        return@withContext if (runAttemptCount < 2) {
          Result.retry()
        } else {
          Result.failure(workDataOf(KEY_ERROR to msg, KEY_FAILED_COUNT to pending.size))
        }
      }

      var uploadedCount = 0
      var failedCount = 0
      var hasRetryableError = false
      var lastError = ""

      for (entity in pending) {
        val attemptAt = System.currentTimeMillis()
        dao.updateAttendanceSyncState(
          id = entity.id,
          syncStatus = AttendanceSyncStatus.SYNCING,
          attemptEpochMs = attemptAt,
          syncError = "",
        )
        val office = dao.getOffice(entity.officeId)?.toDomain()
        val uploadResult =
          try {
            uploader.uploadAttendanceRecord(entity.toDomain(), office)
          } catch (e: CancellationException) {
            dao.updateAttendanceSyncState(
              id = entity.id,
              syncStatus = AttendanceSyncStatus.PENDING,
              attemptEpochMs = attemptAt,
              syncError = "",
            )
            throw e
          }

        if (uploadResult.isSuccess) {
          uploadedCount++
          dao.updateAttendanceSyncState(
            id = entity.id,
            syncStatus = AttendanceSyncStatus.SYNCED,
            attemptEpochMs = System.currentTimeMillis(),
            syncError = "",
          )
        } else {
          failedCount++
          val err = uploadResult.exceptionOrNull()
          val errMsg = (err?.localizedMessage ?: err?.message ?: "Upload failed").take(200)
          lastError = errMsg
          dao.updateAttendanceSyncState(
            id = entity.id,
            syncStatus = AttendanceSyncStatus.FAILED,
            attemptEpochMs = System.currentTimeMillis(),
            syncError = errMsg,
          )
          if (isRetryableFailure(err)) {
            hasRetryableError = true
          }
        }
      }

      val finishedAt = System.currentTimeMillis()
      val summary =
        if (failedCount == 0) {
          "Synced $uploadedCount attendance record(s) to Firestore"
        } else {
          "Synced $uploadedCount, $failedCount pending retry: $lastError"
        }
      preferences.setLastSyncResult(finishedAt, summary)

      when {
        failedCount == 0 ->
          Result.success(
            workDataOf(
              KEY_UPLOADED_COUNT to uploadedCount,
              KEY_FAILED_COUNT to 0,
            )
          )
        hasRetryableError && runAttemptCount < MAX_RETRY_ATTEMPTS ->
          Result.retry()
        else ->
          Result.failure(
            workDataOf(
              KEY_UPLOADED_COUNT to uploadedCount,
              KEY_FAILED_COUNT to failedCount,
              KEY_ERROR to lastError,
            )
          )
      }
    }

  private fun isRetryableFailure(error: Throwable?): Boolean {
    if (error == null) return true
    if (error is IOException) return true
    val fsErr =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    return when (fsErr?.code) {
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      FirebaseFirestoreException.Code.INVALID_ARGUMENT,
      FirebaseFirestoreException.Code.UNAUTHENTICATED -> false
      else -> true
    }
  }

  companion object {
    private const val TAG = "AttendanceSyncWorker"
    private const val MAX_RETRY_ATTEMPTS = 4
    const val KEY_UPLOADED_COUNT = "uploaded_count"
    const val KEY_FAILED_COUNT = "failed_count"
    const val KEY_ERROR = "sync_error"
  }
}
