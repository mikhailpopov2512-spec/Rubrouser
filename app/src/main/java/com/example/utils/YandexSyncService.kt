package com.example.utils

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Service for Yandex Sync Adapter that handles cloud sync under background workers.
 */
class YandexSyncService : Service() {

    private lateinit var syncAdapter: YandexSyncAdapter

    override fun onCreate() {
        super.onCreate()
        syncAdapter = YandexSyncAdapter(applicationContext, true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return syncAdapter.syncAdapterBinder
    }

    class YandexSyncAdapter(context: Context, autoInitialize: Boolean) :
        AbstractThreadedSyncAdapter(context, autoInitialize) {

        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        override fun onPerformSync(
            account: Account?,
            extras: Bundle?,
            authority: String?,
            provider: ContentProviderClient?,
            syncResult: SyncResult?
        ) {
            // Invoked strictly in background thread
            Log.d("YandexSyncAdapter", "Starting background sync for Yandex ID...")
            
            try {
                val accountManager = AccountManager.get(context)
                val token = if (account != null) {
                    try {
                        accountManager.peekAuthToken(account, "yandex_token") ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                } else ""

                // Sync bookmarks
                syncBookmarks(token, accountManager, account)

                // Sync passwords
                syncPasswords(token, accountManager, account)

                Log.d("YandexSyncAdapter", "Background sync completed successfully")
            } catch (e: Exception) {
                Log.e("YandexSyncAdapter", "Error during sync performer", e)
                // Ignored to prevent crashes and maintain stability
            }
        }

        private fun syncBookmarks(token: String, accountManager: AccountManager, account: Account?) {
            try {
                if (token.isEmpty()) return
                val request = Request.Builder()
                    .url("https://api.rosbrowser.ru/sync/bookmarks")
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 401 && account != null) {
                        Log.w("YandexSyncAdapter", "Token expired (401), invalidating account token...")
                        try {
                            accountManager.invalidateAuthToken(account.type, token)
                        } catch (t: Throwable) {
                            Log.e("YandexSyncAdapter", "Failed to invalidate auth token safely", t)
                        }
                    } else if (!response.isSuccessful) {
                        Log.e("YandexSyncAdapter", "Bookmarks sync failed with response: ${response.code}")
                    } else {
                        Log.d("YandexSyncAdapter", "Bookmarks synchronised")
                    }
                }
            } catch (e: Throwable) {
                Log.e("YandexSyncAdapter", "Bookmarks sync error caught safely", e)
            }
        }

        private fun syncPasswords(token: String, accountManager: AccountManager, account: Account?) {
            try {
                if (token.isEmpty()) return
                val request = Request.Builder()
                    .url("https://api.rosbrowser.ru/sync/passwords")
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 401 && account != null) {
                        Log.w("YandexSyncAdapter", "Token expired (401), invalidating account token...")
                        try {
                            accountManager.invalidateAuthToken(account.type, token)
                        } catch (t: Throwable) {
                            Log.e("YandexSyncAdapter", "Failed to invalidate auth token safely", t)
                        }
                    } else if (!response.isSuccessful) {
                        Log.e("YandexSyncAdapter", "Passwords sync failed with response: ${response.code}")
                    } else {
                        Log.d("YandexSyncAdapter", "Passwords synchronised")
                    }
                }
            } catch (e: Throwable) {
                Log.e("YandexSyncAdapter", "Passwords sync error caught safely", e)
            }
        }
    }
}
