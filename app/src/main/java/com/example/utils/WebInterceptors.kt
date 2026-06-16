package com.example.utils

import android.net.Uri
import android.util.Log
import com.example.data.database.BlockedUrl
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.flow.firstOrNull
import java.net.URL

object WebInterceptors {

    private class CacheEntry(val matched: BlockedUrl?)

    // Simple cache for checked URLs to boost performance
    private val blockCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    /**
     * Checks if a domain/url is blocked.
     * Caches hits to prevent slow database lookups on every image/subresource.
     */
    suspend fun checkRknBlock(
        urlStr: String,
        repository: BrowserRepository,
        onBlockDetected: suspend (BlockedUrl) -> Unit
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (urlStr.startsWith("about:") || urlStr.startsWith("data:") || urlStr.contains("blocked_stub")) {
            return@withContext false
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

        // Cache lookup
        val cached = blockCache[host]
        if (cached != null) {
            val cachedResult = cached.matched
            if (cachedResult != null) {
                onBlockDetected(cachedResult)
                return@withContext true
            }
            return@withContext false
        }

        // DB lookup
        val matched = repository.checkBlockedUrl(urlStr)
        blockCache[host] = CacheEntry(matched)

        if (matched != null) {
            onBlockDetected(matched)
            return@withContext true
        }
        return@withContext false
    }

    /**
     * Clears checked cache
     */
    fun clearCache() {
        blockCache.clear()
    }

    /**
     * Comprehensive ad-block matching list containing common Russian and global trackers, counters, and ad networks
     */
    private val adDomains = listOf(
        "google-analytics.com",
        "googletagmanager.com",
        "doubleclick.net",
        "analytics.google.com",
        "adservice.google.com",
        "googleads.g.doubleclick.net",
        "stats.g.doubleclick.net",
        "mc.yandex.ru", // Yandex Metrika
        "clck.yandex.ru", 
        "an.yandex.ru", // Yandex Partner Ads
        "ads.adfox.ru",
        "ad.adriver.ru",
        "top-fwz1.mail.ru", // Mail.ru counter
        "scorecardresearch.com",
        "adnxs.com",
        "ads.vk.com", // VK Ads
        "tns-counter.ru", // Mediascope tracker
        "criteo.com",
        "hotjar.com",
        "amplitude.com",
        "mixpanel.com"
    )

    fun isAdRequest(urlStr: String): Boolean {
        val lowercaseUrl = urlStr.lowercase()
        // Check for common scripts and tracking paths embedded in URLs
        if (lowercaseUrl.contains("/metrika.js") || 
            lowercaseUrl.contains("/analytics.js") || 
            lowercaseUrl.contains("/gtm.js") ||
            lowercaseUrl.contains("yandex.ru/clck") ||
            lowercaseUrl.contains("/adservice?") ||
            lowercaseUrl.contains("/pagead/") ||
            lowercaseUrl.contains("partner.googleadservices")
        ) {
            return true
        }

        val host = try {
            val cleanStr = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                "http://$urlStr"
            } else {
                urlStr
            }
            URL(cleanStr).host?.lowercase() ?: ""
        } catch (e: Exception) {
            return false
        }

        return adDomains.any { adDomain ->
            host == adDomain || host.endsWith(".$adDomain")
        }
    }

    /**
     * Renders matching HTML for blocked page as requested by user.
     */
    fun generateBlockedPageHtml(url: String, reason: String, law: String): String {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Доступ ограничен</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background-color: #0f1013;
                        color: #e2e8f0;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        text-align: center;
                    }
                    .container {
                        max-width: 580px;
                        padding: 40px 24px;
                        background: rgba(30, 41, 59, 0.4);
                        border-radius: 16px;
                        border: 1px solid rgba(239, 68, 68, 0.3);
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
                        backdrop-filter: blur(10px);
                        margin: 16px;
                    }
                    .icon {
                        font-size: 64px;
                        margin-bottom: 24px;
                        color: #ef4444;
                        animation: pulse 2s infinite;
                    }
                    h1 {
                        font-size: 28px;
                        color: #ef4444;
                        margin: 0 0 16px 0;
                        letter-spacing: 1px;
                        font-weight: 800;
                    }
                    .warning-text {
                        font-size: 16px;
                        line-height: 1.6;
                        color: #cbd5e1;
                        margin-bottom: 24px;
                    }
                    .meta-info {
                        text-align: left;
                        background: rgba(15, 16, 20, 0.6);
                        padding: 16px;
                        border-radius: 8px;
                        border-left: 4px solid #ef4444;
                        font-size: 14px;
                        line-height: 1.5;
                        margin-bottom: 30px;
                    }
                    .meta-item {
                        margin-bottom: 8px;
                    }
                    .meta-item:last-child {
                        margin-bottom: 0;
                    }
                    .meta-label {
                        color: #94a3b8;
                        font-weight: 600;
                    }
                    a.btn {
                        display: inline-block;
                        background-color: #ef4444;
                        color: #ffffff;
                        text-decoration: none;
                        padding: 12px 28px;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: background-color 0.2s, transform 0.1s;
                        border: none;
                        cursor: pointer;
                        margin: 4px;
                    }
                    a.btn:hover {
                        background-color: #dc2626;
                    }
                    a.btn-secondary {
                        background-color: transparent;
                        border: 1px solid #475569;
                        color: #94a3b8;
                    }
                    a.btn-secondary:hover {
                        background-color: rgba(255, 255, 255, 0.05);
                        color: #cbd5e1;
                    }
                    .footer {
                        margin-top: 30px;
                        font-size: 12px;
                        color: #64748b;
                    }
                    @keyframes pulse {
                        0% { transform: scale(1); }
                        50% { transform: scale(1.05); }
                        100% { transform: scale(1); }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">🔍</div>
                    <h1>ДОСТУП ОГРАНИЧЕН</h1>
                    <p class="warning-text">
                        Доступ к запрашиваемому интернет-ресурсу ограничен в соответствии с законодательством Российской Федерации.
                    </p>
                    
                    <div class="meta-info">
                        <div class="meta-item">
                            <span class="meta-label">Заблокированный адрес:</span> 
                            <span style="color: #ef4444; word-break: break-all;">$url</span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-label">Основание:</span> $reason
                        </div>
                        <div class="meta-item">
                            <span class="meta-label">Нормативный акт:</span> Закон об информации, информационных технологиях и о защите информации ($law)
                        </div>
                    </div>

                    <a href="https://blocklist.rkn.gov.ru" class="btn" target="_blank">Проверить в реестре РКН</a>
                    <a href="about:home" class="btn btn-secondary">На главную</a>
                    
                    <div class="footer">
                        Национальная система фильтрации трафика • Росбраузер
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
