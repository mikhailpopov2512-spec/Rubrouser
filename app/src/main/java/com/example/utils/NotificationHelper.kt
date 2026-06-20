package com.example.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.MainActivity
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "fsb_monitoring_channel"
    private const val CHANNEL_NAME = "ФСБ Уведомления"
    private const val NOTIFICATION_ID = 1007
    private const val WORK_NAME = "FsbHourlyMonitoringWork"

    /**
     * Set up Notification Channel for Android O and above.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Разрешение на мониторинг безопасности устройства ФСБ"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created successfully.")
        }
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Requests POST_NOTIFICATIONS permission from the activity.
     */
    fun requestNotificationPermission(activity: Activity, requestCode: Int = 101) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    requestCode
                )
                Log.d(TAG, "Requesting notification permission from user.")
            }
        }
    }

    /**
     * Schedule the periodic 1-hour WorkManager background worker.
     */
    fun scheduleHourlyFsbAlert(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<FsbWorker>(
            1, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing to avoid resetting hourly cycle
            periodicWorkRequest
        )
        Log.d(TAG, "WorkManager scheduled one-hour FSB alerts.")
    }

    /**
     * Fires an immediate FSB alert (used to instantly demonstrate/simulate the hourly notification).
     */
    fun fireFsbAlertNow(context: Context) {
        // Double check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            Log.e(TAG, "Cannot send FSB alert - missing POST_NOTIFICATIONS permission.")
            return
        }

        // Setup intent to launch main browser activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RF Coat of arms lookalike or security warning emblem
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("ФСБ России • Внимание")
            .setContentText("Ваш телефон прослушивает ФСБ, не сопротивляйтесь")
            .setSubText("Система мониторинга")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Ваш телефон прослушивает ФСБ, не сопротивляйтесь.\n" +
                "Устройство внесено в единый реестр суверенного контроля связи Комитета Безопасности."
            ))
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Fired FSB notification instantly.")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while firing notification", e)
        }
    }
}
