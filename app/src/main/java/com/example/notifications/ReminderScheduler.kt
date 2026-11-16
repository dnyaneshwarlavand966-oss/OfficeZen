// OfficeZen daily check-in & month-end reminder scheduler
package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object NotificationHelper {
  const val CHANNEL_ID = "officezen_reminders"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "OfficeZen Reminders & Alerts",
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = "Attendance reminders, leave approvals, and month-end owner report alerts."
        }
      val mgr = context.getSystemService(NotificationManager::class.java)
      mgr?.createNotificationChannel(channel)
    }
  }

  fun showNotification(context: Context, id: Int, title: String, body: String) {
    ensureChannel(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    val openIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pending =
      PendingIntent.getActivity(
        context,
        id,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .build()
    NotificationManagerCompat.from(context).notify(id, notification)
  }
}

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_CHECK_IN
    val title =
      when (type) {
        TYPE_MONTH_END -> "OfficeZen: Month-End Report Ready"
        else -> "OfficeZen: Good Morning Check-In"
      }
    val body =
      when (type) {
        TYPE_MONTH_END -> "Review this month's salary total, attendance summary & chai expenses and share via WhatsApp/Email."
        else -> "Tap to complete your selfie attendance check-in for today."
      }
    NotificationHelper.showNotification(context, type.hashCode(), title, body)
  }

  companion object {
    const val EXTRA_TYPE = "reminder_type"
    const val TYPE_CHECK_IN = "check_in"
    const val TYPE_MONTH_END = "month_end"
  }
}

object ReminderScheduler {
  fun scheduleDailyAndMonthEndReminders(context: Context, enabled: Boolean) {
    NotificationHelper.ensureChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val checkInIntent =
      Intent(context, ReminderReceiver::class.java).putExtra(
        ReminderReceiver.EXTRA_TYPE,
        ReminderReceiver.TYPE_CHECK_IN,
      )
    val checkInPending =
      PendingIntent.getBroadcast(
        context,
        101,
        checkInIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    if (!enabled) {
      alarmManager.cancel(checkInPending)
      return
    }

    val nextCheckIn =
      Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 15)
        set(Calendar.SECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
          add(Calendar.DAY_OF_MONTH, 1)
        }
      }
    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      nextCheckIn.timeInMillis,
      AlarmManager.INTERVAL_DAY,
      checkInPending,
    )
  }
}
