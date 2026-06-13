package com.example.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.database.BrowserDatabase
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class BlocklistUpdateService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val CHANNEL_ID = "blocklist_update_channel"
        private const val NOTIFICATION_ID = 4096
        private const val TAG = "BlocklistUpdateService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start Foreground Service within 5s mandatory threshold to avoid crash on Oreo+
        val notification = createNotification("Обновление баз данных...", "Загрузка актуального реестра ограничений РКН")
        startForeground(NOTIFICATION_ID, notification)

        // Perform the async task in high safety scope
        serviceScope.launch {
            try {
                Log.d(TAG, "Service started, downloading json blocklist...")
                val db = BrowserDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO + SupervisorJob()))
                val repo = BrowserRepository(db.browserDao())

                val request = Request.Builder()
                    .url("https://api.rosbrowser.ru/rkn_config.json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "HTTP unsucessful response: ${response.code}")
                        return@use
                    }
                    val jsonStr = response.body?.string() ?: ""
                    if (jsonStr.isNotBlank()) {
                        parseAndSaveBlocklist(jsonStr, repo)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update list task failed safely. Leaving existing list intact.", e)
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun parseAndSaveBlocklist(jsonStr: String, repository: BrowserRepository) {
        try {
            val jsonArray = JSONArray(jsonStr)
            repository.clearBlockedUrls() // Safe cleanup before rewrite
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pattern = obj.getString("pattern")
                val reason = obj.getString("reason")
                repository.addBlockedUrl(pattern, reason)
            }
            Log.d(TAG, "Successfully parsed and saved ${jsonArray.length()} block entries.")
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error failed gracefully", e)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Обновление фильтров"
            val descriptionText = "Служба обновления базы запрещённых сайтов Роскомнадзора"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
