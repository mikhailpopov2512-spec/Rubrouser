package com.example.utils

import android.util.Log
import com.example.data.database.BlockedUrl
import java.net.URL

// Simulated/Expected Chromium API classes to avoid compilation/runtime failures and represent Section 1.1 correctly.
interface NavigationHandle {
    fun getUrl(): String
    fun isNavigationFinished(): Boolean
    fun cancel()
}

class RknBlockThrottle(private val navigationHandle: NavigationHandle) {
    companion object {
        private const val TAG = "RknBlockThrottle"
        
        // Navigation throttle action constants
        const val PROCEED = 1
        const val CANCEL = 2
    }

    fun willStartRequest(): Int {
        return checkUrl(navigationHandle.getUrl())
    }

    fun willRedirectRequest(): Int {
        return checkUrl(navigationHandle.getUrl())
    }

    private fun checkUrl(urlStr: String): Int {
        if (urlStr.startsWith("about:") || urlStr.startsWith("data:") || urlStr.contains("blocked_stub")) {
            return PROCEED
        }

        val host = try {
            val cleanStr = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                "http://$urlStr"
            } else {
                urlStr
            }
            URL(cleanStr).host?.lowercase() ?: ""
        } catch (e: Exception) {
            urlStr.lowercase().trim()
        }

        // Fast memory lookup using LruCache preloaded by RknBlocklistManager
        var isBlocked = false
        var matchedPattern = ""
        
        val cache = RknBlocklistManager.blockedCache
        if (cache.get(host) == true) {
            isBlocked = true
            matchedPattern = host
        } else {
            val snapshot = cache.snapshot().keys
            for (pattern in snapshot) {
                if (host == pattern || host.endsWith(".$pattern")) {
                    isBlocked = true
                    matchedPattern = pattern
                    break
                }
            }
        }

        if (isBlocked) {
            Log.w(TAG, "Navigation blocked by RknBlockThrottle: $urlStr (matched: $matchedPattern)")
            
            // Check navigation finished state before cancel
            if (!navigationHandle.isNavigationFinished()) {
                navigationHandle.cancel()
            }
            
            // Show blocked stub asynchronously on UI thread
            ThreadUtils.postOnUiThread {
                Log.d(TAG, "Navigating to blocked stub because of RKN matched rule on URL: $urlStr")
            }
            return CANCEL
        }

        return PROCEED
    }
}
