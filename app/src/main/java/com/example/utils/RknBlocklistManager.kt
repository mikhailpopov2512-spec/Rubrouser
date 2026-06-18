package com.example.utils

import android.content.Context
import android.util.LruCache
import android.util.Log
import com.example.data.database.BlockedUrl
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager responsible for asynchronous preloading and updating of RKN blocklists.
 */
object RknBlocklistManager {
    private const val TAG = "RknBlocklistManager"
    
    // Fast memory cache for blocked pattern lookups
    val blockedCache = LruCache<String, Boolean>(2048)
    
    // Detailed info cache
    val blockedInfoCache = LruCache<String, BlockedUrl>(2048)

    fun initialize(context: Context, repository: BrowserRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Scheduling blocklist update task...")
                BlocklistUpdateWorker.schedule(context.applicationContext)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to schedule blocklist updates", t)
            }

            try {
                Log.d(TAG, "Starting asynchronous blocklist preloading...")
                repository.allBlockedUrls.collect { list ->
                    blockedCache.evictAll()
                    blockedInfoCache.evictAll()
                    for (item in list) {
                        val pattern = item.pattern.lowercase().trim()
                        blockedCache.put(pattern, true)
                        blockedInfoCache.put(pattern, item)
                    }
                    Log.d(TAG, "Asynchronously preloaded ${list.size} RKN patterns to memory.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading RKN blocklist", e)
            }
        }
    }
}
