package com.example.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.data.database.BrowserDatabase
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class BlocklistUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = BrowserDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO + SupervisorJob()))
            val repo = BrowserRepository(db.browserDao())
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.rosbrowser.ru/rkn_config.json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.retry()
                }
                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isNotBlank()) {
                    val jsonArray = JSONArray(jsonStr)
                    repo.clearBlockedUrls()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val pattern = obj.getString("pattern")
                        val reason = obj.getString("reason")
                        repo.addBlockedUrl(pattern, reason)
                    }
                    try {
                        BrowserNotificationHelper.showNotification(
                            applicationContext,
                            id = 1003,
                            title = "Реестр РКН обновлен",
                            message = "База ограничений успешно синхронизирована (каждые 12 часов)."
                        )
                    } catch (t: Throwable) {
                        android.util.Log.e("BlocklistUpdateWorker", "Failed to show notification", t)
                    }
                }
            }
            Result.success()
        } catch (e: Throwable) {
            android.util.Log.e("BlocklistUpdateWorker", "Update task failed", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(true)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<BlocklistUpdateWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "BlocklistUpdateWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                android.util.Log.d("BlocklistUpdateWorker", "Scheduled periodic blocklist update every 12h with charging and network constraints.")
            } catch (t: Throwable) {
                android.util.Log.e("BlocklistUpdateWorker", "Failed to schedule periodic work", t)
            }
        }
    }
}
