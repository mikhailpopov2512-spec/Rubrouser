package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class FsbWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("FsbWorker", "Triggering periodic FSB notification task...")
        return try {
            NotificationHelper.fireFsbAlertNow(context)
            Result.success()
        } catch (e: Exception) {
            Log.e("FsbWorker", "Error sending periodic FSB notification", e)
            Result.retry()
        }
    }
}
