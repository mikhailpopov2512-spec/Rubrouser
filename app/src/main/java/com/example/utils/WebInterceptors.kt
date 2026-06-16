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

    fun extractHost(urlStr: String): String {
        if (urlStr.isBlank()) return ""
        try {
            var startIndex = 0
            val lower = urlStr.lowercase()
            if (lower.startsWith("http://")) {
                startIndex = 7
            } else if (lower.startsWith("https://")) {
                startIndex = 8
            }
            var endIndex = urlStr.indexOf('/', startIndex)
            if (endIndex == -1) {
                endIndex = urlStr.indexOf('?', startIndex)
            }
            if (endIndex == -1) {
                endIndex = urlStr.indexOf('#', startIndex)
            }
            var host = if (endIndex == -1) {
                urlStr.substring(startIndex)
            } else {
                urlStr.substring(startIndex, endIndex)
            }
            val colonIdx = host.indexOf(':')
            if (colonIdx != -1) {
                host = host.substring(0, colonIdx)
            }
            return host.lowercase().trim()
        } catch (e: Exception) {
            return try {
                android.net.Uri.parse(urlStr).host?.lowercase() ?: ""
            } catch (ex: Exception) {
                ""
            }
        }
    }

    fun getHostAndParents(host: String): List<String> {
        val result = java.util.ArrayList<String>()
        val cleanHost = host.trim().lowercase()
        if (cleanHost.isBlank()) return result
        result.add(cleanHost)
        var current = cleanHost
        while (true) {
            val nextDot = current.indexOf('.')
            if (nextDot == -1 || nextDot == current.length - 1) break
            current = current.substring(nextDot + 1)
            if (current.contains('.')) {
                result.add(current)
            } else {
                break
            }
        }
        return result
    }

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

        val host = extractHost(urlStr)
        if (host.isBlank()) return@withContext false

        // 1. FAST MEMORY CHECK USING LRU CACHED SUBDOMAINS
        val parents = getHostAndParents(host)
        for (parent in parents) {
            val cachedUrl = RknBlocklistManager.blockedInfoCache.get(parent)
            if (cachedUrl != null) {
                onBlockDetected(cachedUrl)
                return@withContext true
            }
        }

        // 2. BACKUP LOCAL INTERCEPTOR CACHE LOOKUP
        val cached = blockCache[host]
        if (cached != null) {
            val cachedResult = cached.matched
            if (cachedResult != null) {
                onBlockDetected(cachedResult)
                return@withContext true
            }
            return@withContext false
        }

        // 3. FAST DATABASE DELEGATE LOOKUP
        val matched = repository.checkBlockedUrl(urlStr)
        blockCache[host] = CacheEntry(matched)

        if (matched != null) {
            val cleanPattern = matched.pattern.lowercase().trim()
            RknBlocklistManager.blockedInfoCache.put(cleanPattern, matched)
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

        val host = extractHost(urlStr)
        if (host.isBlank()) return false

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
                <title>Доступ ограничен • Росбраузер</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background: radial-gradient(circle at center, rgba(10, 11, 14, 0.94) 30%, rgba(5, 5, 7, 0.98) 100%),
                                    linear-gradient(180deg, rgba(255,255,255,0.07) 0%, rgba(255,255,255,0.07) 33.3%, rgba(0,57,166,0.08) 33.3%, rgba(0,57,166,0.08) 66.6%, rgba(213,43,30,0.08) 66.6%, rgba(213,43,30,0.08) 100%);
                        background-size: cover;
                        background-attachment: fixed;
                        color: #f1f5f9;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        overflow: hidden;
                    }
                    .container {
                        max-width: 540px;
                        width: 90%;
                        padding: 40px 30px;
                        background: rgba(15, 23, 42, 0.75);
                        border-radius: 20px;
                        border: 1px solid rgba(255, 255, 255, 0.1);
                        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6), inset 0 0 20px rgba(255, 255, 255, 0.05);
                        backdrop-filter: blur(25px) saturate(140%);
                        -webkit-backdrop-filter: blur(25px) saturate(140%);
                        text-align: center;
                        animation: fadeIn 0.4s ease-out;
                    }
                    .badge {
                        display: inline-flex;
                        align-items: center;
                        background: rgba(239, 68, 68, 0.15);
                        border: 1px solid rgba(239, 68, 68, 0.4);
                        color: #f87171;
                        font-weight: 700;
                        font-size: 11px;
                        letter-spacing: 1.5px;
                        padding: 6px 16px;
                        border-radius: 20px;
                        margin-bottom: 24px;
                        text-transform: uppercase;
                    }
                    h1 {
                        font-size: 26px;
                        color: #ffffff;
                        margin: 0 0 12px 0;
                        letter-spacing: 1px;
                        font-weight: 900;
                        text-shadow: 0 2px 10px rgba(0,0,0,0.3);
                    }
                    .sub-title {
                        font-size: 14px;
                        color: #94a3b8;
                        margin-bottom: 30px;
                        line-height: 1.5;
                    }
                    .blocked-card {
                        background: rgba(8, 10, 15, 0.5);
                        border-left: 4px solid #ef4444;
                        padding: 16px 20px;
                        border-radius: 8px;
                        text-align: left;
                        margin-bottom: 30px;
                    }
                    .card-item {
                        margin-bottom: 10px;
                        font-size: 13px;
                    }
                    .card-item:last-child {
                        margin-bottom: 0;
                    }
                    .card-label {
                        color: #64748b;
                        font-weight: 700;
                        display: block;
                        margin-bottom: 2px;
                        text-transform: uppercase;
                        font-size: 10px;
                        letter-spacing: 0.8px;
                    }
                    .card-val {
                        color: #e2e8f0;
                        word-break: break-all;
                        font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
                    }
                    .btn-row {
                        display: flex;
                        gap: 12px;
                        justify-content: center;
                        flex-wrap: wrap;
                    }
                    .btn {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
                        color: #ffffff;
                        text-decoration: none;
                        padding: 12px 24px;
                        border-radius: 10px;
                        font-weight: 700;
                        font-size: 14px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
                    }
                    .btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 8px 20px rgba(239, 68, 68, 0.4);
                    }
                    .btn:active {
                        transform: translateY(0);
                    }
                    .btn-secondary {
                        background: rgba(255, 255, 255, 0.08);
                        border: 1px solid rgba(255, 255, 255, 0.1);
                        color: #cbd5e1;
                        box-shadow: none;
                    }
                    .btn-secondary:hover {
                        background: rgba(255, 255, 255, 0.15);
                        border-color: rgba(255, 255, 255, 0.2);
                        color: #ffffff;
                        box-shadow: none;
                    }
                    .footer-brand {
                        margin-top: 35px;
                        font-size: 11px;
                        color: #475569;
                        letter-spacing: 1px;
                        font-weight: 700;
                        text-transform: uppercase;
                    }
                    @keyframes fadeIn {
                        from { opacity: 0; transform: scale(0.96); }
                        to { opacity: 1; transform: scale(1); }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="badge">⚠️ ФЗ-149 Ограничение</div>
                    <h1>ДОСТУП ОГРАНИЧЕН</h1>
                    <div class="sub-title">Навещаемый сетевой узел временно заблокирован Федеральной службой по надзору в сфере связи, информационных технологий и массовых коммуникаций РФ.</div>
                    
                    <div class="blocked-card">
                        <div class="card-item">
                            <span class="card-label">Сетевой адрес:</span>
                            <span class="card-val" style="color: #f87171;">$url</span>
                        </div>
                        <div class="card-item">
                            <span class="card-label">Причина блокировки:</span>
                            <span class="card-val">$reason</span>
                        </div>
                        <div class="card-item">
                            <span class="card-label">Основание закона:</span>
                            <span class="card-val" style="font-size:12px;">ФЗ-149 «Об информации, информационных технологиях и о защите информации» ($law)</span>
                        </div>
                    </div>

                    <div class="btn-row">
                        <a href="https://blocklist.rkn.gov.ru" class="btn" target="_blank">Реестр РКН</a>
                        <a href="about:home" class="btn btn-secondary">Вернуться назад</a>
                    </div>
                    
                    <div class="footer-brand">
                        Суверенный контур безопасности • Росбраузер
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
