package com.example.utils

import android.os.Handler
import android.os.Looper

object ThreadUtils {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun postOnUiThread(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            mainHandler.post(runnable)
        }
    }
}
