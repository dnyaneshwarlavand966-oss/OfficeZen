// WorkManager scheduler and ConnectivityManager network change observer for attendance sync
package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AttendanceSyncScheduler {
  const val UNIQUE_SYNC_WORK_NAME = "officezen_attendance_sync_on_connect"
  const val UNIQUE_PERIODIC_SYNC_NAME = "officezen_attendance_periodic_sync"
  const val TAG_ATTENDANCE_SYNC = "attendance_firestore_sync"

  fun connectedConstraints(): Constraints =
    Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

  fun enqueueSyncWhenConnected(context: Context) {
    val request =
      OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
        .setConstraints(connectedConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .addTag(TAG_ATTENDANCE_SYNC)
        .build()

    WorkManager.getInstance(context.applicationContext)
      .enqueueUniqueWork(
        UNIQUE_SYNC_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request,
      )
  }

  fun ensurePeriodicSyncScheduled(context: Context) {
    val request =
      PeriodicWorkRequestBuilder<AttendanceSyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(connectedConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .addTag(TAG_ATTENDANCE_SYNC)
        .build()

    WorkManager.getInstance(context.applicationContext)
      .enqueueUniquePeriodicWork(
        UNIQUE_PERIODIC_SYNC_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
      )
  }

  fun cancelScheduledSync(context: Context) {
    val wm = WorkManager.getInstance(context.applicationContext)
    wm.cancelUniqueWork(UNIQUE_SYNC_WORK_NAME)
    wm.cancelUniqueWork(UNIQUE_PERIODIC_SYNC_NAME)
  }

  fun observeSyncWorkInfos(context: Context): Flow<List<WorkInfo>> =
    WorkManager.getInstance(context.applicationContext)
      .getWorkInfosForUniqueWorkFlow(UNIQUE_SYNC_WORK_NAME)
}

class NetworkConnectivityObserver(
  context: Context,
  private val onConnectivityRestored: () -> Unit = {},
) {
  private val appContext = context.applicationContext
  private val connectivityManager =
    appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private var registeredCallback: ConnectivityManager.NetworkCallback? = null

  fun startObserving() {
    val cm = connectivityManager ?: return
    if (registeredCallback != null) return

    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          val wasOffline = !_isOnline.value
          val nowOnline = checkCurrentlyOnline() || hasNetworkInternet(network)
          _isOnline.value = nowOnline
          if (nowOnline && wasOffline) {
            onConnectivityRestored()
          } else if (nowOnline) {
            onConnectivityRestored()
          }
        }

        override fun onCapabilitiesChanged(
          network: Network,
          networkCapabilities: NetworkCapabilities,
        ) {
          val wasOffline = !_isOnline.value
          val hasInternet =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
              (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
          _isOnline.value = hasInternet
          if (hasInternet && wasOffline) {
            onConnectivityRestored()
          }
        }

        override fun onLost(network: Network) {
          _isOnline.value = checkCurrentlyOnline()
        }

        override fun onUnavailable() {
          _isOnline.value = false
        }
      }

    registeredCallback = callback
    val request =
      NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    runCatching { cm.registerNetworkCallback(request, callback) }
  }

  fun stopObserving() {
    val cm = connectivityManager ?: return
    val callback = registeredCallback ?: return
    runCatching { cm.unregisterNetworkCallback(callback) }
    registeredCallback = null
  }

  fun checkCurrentlyOnline(): Boolean {
    val cm = connectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    return hasNetworkInternet(activeNetwork)
  }

  private fun hasNetworkInternet(network: Network): Boolean {
    val cm = connectivityManager ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
  }
}
