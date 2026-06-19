package com.example.utils

import android.util.Log
import android.util.LruCache
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object CheburcheckService {
    private const val TAG = "CheburcheckService"

    // Cache entry representing status and timestamp
    private data class CacheEntry(val isBlocked: Boolean, val timestamp: Long)

    // LruCache storing check results, expires after 5 minutes (300000ms)
    private val checkCache = LruCache<String, CacheEntry>(500)
    private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes

    // Specialized HTTP Client with a strict 3-second timeout
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a URL is blocked via cheburcheck.ru API.
     * Uses LruCache for fast lookups.
     * If the service is unreachable or timeouts, falls back to the embedded RKN list.
     * Returns true if blocked, false if allowed.
     */
    suspend fun checkUrl(urlStr: String, repository: BrowserRepository): Boolean = withContext(Dispatchers.IO) {
        if (urlStr.isBlank() || urlStr.startsWith("about:") || urlStr.startsWith("data:") || urlStr.contains("blocked_stub")) {
            return@withContext false
        }

        val cacheKey = WebInterceptors.extractHost(urlStr)
        if (cacheKey.isBlank()) {
            return@withContext false
        }

        // 1. Check LruCache memory hit
        val cached = checkCache.get(cacheKey)
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            if (age < CACHE_DURATION_MS) {
                Log.d(TAG, "LruCache HIT for host '$cacheKey': blocked=${cached.isBlocked}")
                return@withContext cached.isBlocked
            } else {
                Log.d(TAG, "LruCache EXPIRED for host '$cacheKey'")
                checkCache.remove(cacheKey)
            }
        }

        // 2. Query cheburcheck.ru API asynchronously with 3s timeout
        try {
            val encodedUrl = URLEncoder.encode(urlStr, "UTF-8")
            val requestUrl = "https://cheburcheck.ru/api/check?url=$encodedUrl"
            
            val request = Request.Builder()
                .url(requestUrl)
                .header("User-Agent", "RosBrowserProtect/1.0 (Android; Chromium Fork)")
                .get()
                .build()

            Log.d(TAG, "Querying cheburcheck API for URL: $urlStr")
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = JSONObject(bodyString)
                        val status = json.optString("status", "allowed")
                        val isBlocked = status.equals("blocked", ignoreCase = true)
                        
                        Log.d(TAG, "API success for '$cacheKey': status='$status' (blocked=$isBlocked)")
                        
                        // Store in cache with current timestamp
                        checkCache.put(cacheKey, CacheEntry(isBlocked, System.currentTimeMillis()))
                        return@withContext isBlocked
                    }
                } else {
                    Log.w(TAG, "API returned unsuccessful code: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cheburcheck query failed: ${e.message}. Falling back to internal RKN blocklist.", e)
        }

        // 3. FALLBACK: Internal database / RKN block list matching
        Log.d(TAG, "Using internal RKN list fallback check for URL: $urlStr")
        val rknBlocked = WebInterceptors.checkRknBlockSync(urlStr) != null
                || repository.checkBlockedUrl(urlStr) != null

        // Cache the fallback result for safety as well to prevent aggressive network polling
        checkCache.put(cacheKey, CacheEntry(rknBlocked, System.currentTimeMillis()))
        return@withContext rknBlocked
    }

    /**
     * Clears local Cheburcheck cache entries.
     */
    fun clearCache() {
        checkCache.evictAll()
    }

    /**
     * Renders a pristine, modern offline HTML blocked page in beautiful summer design.
     */
    fun generateBlockedPageHtml(url: String): String {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Доступ заблокирован • RosBrowser</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background: radial-gradient(circle at center, #111827 0%, #030712 100%);
                        color: #f3f4f6;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        position: relative;
                        overflow-x: hidden;
                    }
                    /* Subtle Summer Organic elements in backdrop */
                    .summer-glow {
                        position: absolute;
                        width: 300px;
                        height: 300px;
                        background: radial-gradient(circle, rgba(16, 185, 129, 0.08) 0%, rgba(0, 0, 0, 0) 70%);
                        top: 10%;
                        left: 5%;
                        z-index: 1;
                    }
                    .summer-glow-warm {
                        position: absolute;
                        width: 400px;
                        height: 400px;
                        background: radial-gradient(circle, rgba(245, 158, 11, 0.05) 0%, rgba(0, 0, 0, 0) 70%);
                        bottom: 10%;
                        right: 5%;
                        z-index: 1;
                    }
                    .container {
                        max-width: 500px;
                        width: 88%;
                        padding: 35px 25px;
                        background: rgba(17, 24, 39, 0.85);
                        border-radius: 20px;
                        border: 1px solid rgba(255, 255, 255, 0.08);
                        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.7);
                        backdrop-filter: blur(20px);
                        -webkit-backdrop-filter: blur(20px);
                        text-align: center;
                        z-index: 2;
                        animation: slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1);
                    }
                    /* Sovereign Dark-Themed Flag Header */
                    .flag-container {
                        display: flex;
                        flex-direction: column;
                        width: 130px;
                        height: 24px;
                        margin: 0 auto 20px auto;
                        border-radius: 4px;
                        overflow: hidden;
                        opacity: 0.65;
                        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
                        border: 1px solid rgba(255, 255, 255, 0.1);
                    }
                    .flag-stripe {
                        flex: 1;
                        width: 100%;
                    }
                    .flag-white { background-color: #e5e7eb; }
                    .flag-blue { background-color: #1e40af; }
                    .flag-red { background-color: #b91c1c; }
                    
                    /* Visual Summer Decorative Birch Outline */
                    .birch-decor {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        color: #10b981;
                        background: rgba(16, 185, 129, 0.1);
                        border: 1px solid rgba(16, 185, 129, 0.25);
                        font-weight: 700;
                        font-size: 11px;
                        letter-spacing: 1.5px;
                        padding: 6px 14px;
                        border-radius: 12px;
                        margin-bottom: 22px;
                        text-transform: uppercase;
                    }
                    h1 {
                        font-size: 24px;
                        color: #ffffff;
                        margin: 0 0 14px 0;
                        letter-spacing: 1px;
                        font-weight: 900;
                        text-transform: uppercase;
                    }
                    .description {
                        font-size: 13.5px;
                        color: #9ca3af;
                        line-height: 1.55;
                        margin-bottom: 25px;
                    }
                    .details-box {
                        background: rgba(15, 23, 42, 0.6);
                        border-left: 3.5px solid #ef4444;
                        padding: 14px 18px;
                        border-radius: 8px;
                        text-align: left;
                        margin-bottom: 25px;
                    }
                    .box-row {
                        margin-bottom: 8px;
                        font-size: 13px;
                    }
                    .box-row:last-child {
                        margin-bottom: 0;
                    }
                    .label {
                        color: #6b7280;
                        font-weight: 700;
                        display: block;
                        margin-bottom: 2px;
                        text-transform: uppercase;
                        font-size: 10px;
                        letter-spacing: 0.8px;
                    }
                    .value {
                        color: #e5e7eb;
                        word-break: break-all;
                        font-family: SFMono-Regular, Consolas, "Liberation Mono", Menloco, monospace;
                    }
                    .btn-row {
                        display: flex;
                        gap: 12px;
                        justify-content: center;
                    }
                    .btn {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
                        border: 1px solid rgba(255, 255, 255, 0.12);
                        color: #ffffff;
                        text-decoration: none;
                        padding: 12px 28px;
                        border-radius: 10px;
                        font-weight: 700;
                        font-size: 13.5px;
                        cursor: pointer;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
                    }
                    .btn:hover {
                        transform: translateY(-2.5f);
                        border-color: rgba(255, 255, 255, 0.25);
                        background: #374151;
                    }
                    .btn:active {
                        transform: translateY(0);
                    }
                    .btn-primary {
                        background: linear-gradient(135deg, #059669 0%, #10b981 100%);
                        border: none;
                        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
                    }
                    .btn-primary:hover {
                        background: linear-gradient(135deg, #047857 0%, #059669 100%);
                        box-shadow: 0 6px 18px rgba(16, 185, 129, 0.35);
                    }
                    .footer-note {
                        margin-top: 30px;
                        font-size: 10.5px;
                        color: #4b5563;
                        letter-spacing: 0.8px;
                        font-weight: 700;
                        text-transform: uppercase;
                    }
                    @keyframes slideUp {
                        from { opacity: 0; transform: translateY(15px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                </style>
            </head>
            <body>
                <div class="summer-glow"></div>
                <div class="summer-glow-warm"></div>
                
                <div class="container">
                    <!-- Sovereign Dark-Themed Flag Header -->
                    <div class="flag-container">
                        <div class="flag-stripe flag-white"></div>
                        <div class="flag-stripe flag-blue"></div>
                        <div class="flag-stripe flag-red"></div>
                    </div>
                    
                    <div class="birch-decor">🌱 Летний Щит Protect</div>
                    <h1>ДОСТУП ЗАБЛОКИРОВАН</h1>
                    <p class="description">Запрошенный вами веб-сайт ограничен сервисом суверенной фильтрации ввиду наличия потенциальных угроз или несоответствия требованиям безопасности.</p>
                    
                    <div class="details-box">
                        <div class="box-row">
                            <span class="label">Адрес ресурса:</span>
                            <span class="value" style="color: #fca5a5;">$url</span>
                        </div>
                        <div class="box-row">
                            <span class="label">Статус проверки:</span>
                            <span class="value">BLOCKED по решению cheburcheck.ru / Реестр РФ</span>
                        </div>
                    </div>

                    <div class="btn-row">
                        <a onclick="window.history.back();" class="btn btn-primary">Назад</a>
                        <a href="about:home" class="btn">Домой</a>
                    </div>
                    
                    <div class="footer-note">
                        Интеллектуальная система безопасности «Росбраузер»
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
