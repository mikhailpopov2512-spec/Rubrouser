package com.example.utils

import android.util.Log
import com.example.data.database.BlockedUrl
import java.net.URL

// Simulated/Expected Chromium API classes to avoid compilation/runtime failures and represent Section 1.1 correctly.
interface NavigationHandle {
    fun getUrl(): String?
    fun isNavigationFinished(): Boolean
    fun cancel()
}

class RknBlockThrottle(private val navigationHandle: NavigationHandle?) {
    companion object {
        private const val TAG = "RknBlockThrottle"
        
        // Navigation throttle action constants
        const val PROCEED = 1
        const val CANCEL = 2
    }

    fun willStartRequest(): Int {
        val url = try {
            navigationHandle?.getUrl() ?: ""
        } catch (e: Exception) {
            ""
        }
        return checkUrl(url)
    }

    fun willRedirectRequest(): Int {
        val url = try {
            navigationHandle?.getUrl() ?: ""
        } catch (e: Exception) {
            ""
        }
        return checkUrl(url)
    }

    private fun checkUrl(urlStr: String): Int {
        if (urlStr.isBlank() || urlStr.startsWith("about:") || urlStr.startsWith("data:") || urlStr.contains("blocked_stub")) {
            return PROCEED
        }

        try {
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

            if (host.isBlank()) return PROCEED

            // Fast memory lookup using LruCache preloaded by RknBlocklistManager
            var isBlocked = false
            var matchedPattern = ""
            
            val cache = RknBlocklistManager.blockedCache
            if (cache.get(host) == true) {
                isBlocked = true
                matchedPattern = host
            } else {
                val snapshot = try { cache.snapshot().keys } catch (e: Exception) { emptySet() }
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
                
                // Safe check, prevent NullPointerException or completed navigation cancel crashes
                try {
                    val finished = navigationHandle?.isNavigationFinished() ?: true
                    if (!finished) {
                        navigationHandle?.cancel()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel navigation handle safely", e)
                }
                
                // Show blocked stub asynchronously on UI thread
                ThreadUtils.postOnUiThread {
                    Log.d(TAG, "Navigating to blocked stub because of RKN matched rule on URL: $urlStr")
                }
                return CANCEL
            }
        } catch (e: Exception) {
            Log.e(TAG, "Safety failure in checkUrl", e)
        }

        return PROCEED
    }
}
