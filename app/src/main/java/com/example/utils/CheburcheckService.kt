package com.example.utils

import android.util.Log
import android.util.LruCache
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL

object CheburcheckService {
    private const val TAG = "CheburcheckService"

    // 5-minute memory cache mapping host to block status (expiration: 300000ms)
    private val dnsBlockedCache = LruCache<String, Boolean>(2048)
    private val cacheTimestamps = HashMap<String, Long>()
    private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes

    /**
     * Checks if a URL is restricted using:
     * 1) Fast memory LruCache (5 mins duration)
     * 2) Asynchronous DNS check via dns.yandex.ru with 2-second timeout.
     *    If the response contains 127.0.0.1 (or loopback/block sign), it is restricted.
     * 3) Safe fallback to the SQLite local RKN table database.
     */
    suspend fun checkUrl(urlStr: String, repository: BrowserRepository): Boolean = withContext(Dispatchers.IO) {
        if (urlStr.isBlank() || urlStr.startsWith("about:") || urlStr.startsWith("data:") || urlStr.contains("blocked_stub")) {
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

        if (host.isBlank()) {
            return@withContext false
        }

        // 1. Sync check of LruCache for fast feedback
        val cachedBlocked = dnsBlockedCache.get(host)
        val timestamp = cacheTimestamps[host] ?: 0L
        if (cachedBlocked != null && (System.currentTimeMillis() - timestamp) < CACHE_DURATION_MS) {
            Log.d(TAG, "LruCache hit for: $host -> blocked: $cachedBlocked")
            return@withContext cachedBlocked
        }

        // 2. Perform raw DNS check via dns.yandex.ru (77.88.8.8) with 2s timeout
        var resolvedIps: List<String> = emptyList()
        try {
            resolvedIps = queryDnsYandex(host, timeoutMs = 2000)
            Log.d(TAG, "dns.yandex.ru lookup for '$host' returned: $resolvedIps")
        } catch (e: Exception) {
            Log.e(TAG, "DNS query to dns.yandex.ru failed, falling back", e)
        }

        var isDnsBlocked = false
        if (resolvedIps.isNotEmpty()) {
            for (ip in resolvedIps) {
                // If the local provider or dns.yandex.ru returns 127.0.0.1 or similar indicators
                if (ip == "127.0.0.1" || ip.startsWith("127.0.0.")) {
                    isDnsBlocked = true
                    Log.w(TAG, "Domain $host detected as BLOCKED via DNS resolving to local redirect: $ip")
                    break
                }
            }
        }

        // 3. Cache the DNS result if successful
        if (resolvedIps.isNotEmpty() || isDnsBlocked) {
            dnsBlockedCache.put(host, isDnsBlocked)
            cacheTimestamps[host] = System.currentTimeMillis()
            if (isDnsBlocked) {
                return@withContext true
            }
        }

        // 4. FALLBACK: Query the SQLite Local Database (Prepopulated / RKN table list check)
        Log.d(TAG, "Using internal SQLite list fallback check for host: $host")
        val isLocalBlocked = repository.checkBlockedUrl(urlStr) != null || WebInterceptors.checkRknBlockSync(urlStr) != null
        
        // Populate cache for robustness
        dnsBlockedCache.put(host, isLocalBlocked)
        cacheTimestamps[host] = System.currentTimeMillis()

        return@withContext isLocalBlocked
    }

    /**
     * Helper to perform raw UDP DNS resolution directly to Yandex DNS (77.88.8.8)
     */
    private fun queryDnsYandex(domain: String, timeoutMs: Int): List<String> {
        val ips = mutableListOf<String>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val baos = ByteArrayOutputStream()
            // Header
            baos.write(byteArrayOf(0x12.toByte(), 0x34.toByte())) // Transaction ID
            baos.write(byteArrayOf(0x01.toByte(), 0x00.toByte())) // Flags: Standard recursive query
            baos.write(byteArrayOf(0x00.toByte(), 0x01.toByte())) // Questions: 1
            baos.write(byteArrayOf(0x00.toByte(), 0x00.toByte())) // Answer RRs: 0
            baos.write(byteArrayOf(0x00.toByte(), 0x00.toByte())) // Authority RRs: 0
            baos.write(byteArrayOf(0x00.toByte(), 0x00.toByte())) // Additional RRs: 0

            // Question: QNAME
            val parts = domain.split(".")
            for (part in parts) {
                if (part.isEmpty()) continue
                val bytes = part.toByteArray(Charsets.US_ASCII)
                baos.write(bytes.size)
                baos.write(bytes)
            }
            baos.write(0) // Zero length octet terminator

            // QTYPE: A (0x0001)
            baos.write(byteArrayOf(0x00.toByte(), 0x01.toByte()))
            // QCLASS: IN (0x0001)
            baos.write(byteArrayOf(0x00.toByte(), 0x01.toByte()))

            val queryData = baos.toByteArray()
            val yandexDnsIp = InetAddress.getByName("77.88.8.8") // dns.yandex.ru
            val packet = DatagramPacket(queryData, queryData.size, yandexDnsIp, 53)
            socket.send(packet)

            val buffer = ByteArray(1024)
            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)

            val responseData = responsePacket.data
            val responseLen = responsePacket.length

            if (responseLen > 12) {
                // Confirm matching Transaction ID
                if (responseData[0] == 0x12.toByte() && responseData[1] == 0x34.toByte()) {
                    val questions = ((responseData[4].toInt() and 0xFF) shl 8) or (responseData[5].toInt() and 0xFF)
                    val answers = ((responseData[6].toInt() and 0xFF) shl 8) or (responseData[7].toInt() and 0xFF)

                    var ptr = 12
                    // Skip Questions segment
                    for (q in 0 until questions) {
                        while (ptr < responseLen && responseData[ptr] != 0.toByte()) {
                            ptr += (responseData[ptr].toInt() and 0xFF) + 1
                        }
                        ptr += 5 // Skip zero byte, QTYPE (2b) and QCLASS (2b)
                    }

                    // Parse Answer segment
                    for (a in 0 until answers) {
                        if (ptr + 12 > responseLen) break

                        // Skip NAME identifier (could be compression byte ptr)
                        if ((responseData[ptr].toInt() and 0xC0) == 0xC0) {
                            ptr += 2
                        } else {
                            while (ptr < responseLen && responseData[ptr] != 0.toByte()) {
                                ptr += (responseData[ptr].toInt() and 0xFF) + 1
                            }
                            ptr += 1
                        }

                        if (ptr + 10 > responseLen) break
                        val type = ((responseData[ptr].toInt() and 0xFF) shl 8) or (responseData[ptr + 1].toInt() and 0xFF)
                        val rclass = ((responseData[ptr + 2].toInt() and 0xFF) shl 8) or (responseData[ptr + 3].toInt() and 0xFF)
                        val rdLength = ((responseData[ptr + 8].toInt() and 0xFF) shl 8) or (responseData[ptr + 9].toInt() and 0xFF)
                        ptr += 10

                        if (type == 1 && rclass == 1 && rdLength == 4) { // TYPE A, Class IN, IPv4 length 4
                            if (ptr + 4 <= responseLen) {
                                val ip = "${responseData[ptr].toInt() and 0xFF}.${responseData[ptr + 1].toInt() and 0xFF}.${responseData[ptr + 2].toInt() and 0xFF}.${responseData[ptr + 3].toInt() and 0xFF}"
                                ips.add(ip)
                            }
                        }
                        ptr += rdLength
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryDnsYandex socket query error: ${e.message}")
        } finally {
            socket?.close()
        }
        return ips
    }

    /**
     * Clears DNS caching entries
     */
    fun clearCache() {
        dnsBlockedCache.evictAll()
        cacheTimestamps.clear()
    }

    /**
     * Renders a pristine visual summer themed lock page with a darkened tricolor flag,
     * beautiful floral ambient accents, a "Назад" button, and a prominent legislative 149-ФЗ Law link.
     */
    fun generateBlockedPageHtml(url: String): String {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Доступ ограничен • Нарушение 149-ФЗ</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
                        background: radial-gradient(circle at center, #0B0F19 0%, #03050C 100%);
                        color: #E2E8F0;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        position: relative;
                        overflow: hidden;
                    }
                    /* Beautiful natural summer vibes decorative glowing overlay */
                    .sun-glow {
                        position: absolute;
                        width: 50vw;
                        height: 50vw;
                        background: radial-gradient(circle, rgba(16, 185, 129, 0.04) 0%, rgba(245, 158, 11, 0.02) 50%, rgba(0, 0, 0, 0) 100%);
                        top: -10%;
                        right: -10%;
                        z-index: 1;
                        pointer-events: none;
                    }
                    .meadow-glow {
                        position: absolute;
                        width: 60vw;
                        height: 60vw;
                        background: radial-gradient(circle, rgba(34, 197, 94, 0.03) 0%, rgba(0, 0, 0, 0) 70%);
                        bottom: -20%;
                        left: -20%;
                        z-index: 1;
                        pointer-events: none;
                    }
                    .card {
                        max-width: 480px;
                        width: 90%;
                        padding: 40px 30px;
                        background: rgba(15, 23, 42, 0.88);
                        border-radius: 24px;
                        border: 1px solid rgba(255, 255, 255, 0.07);
                        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8), inset 0 1px 0 rgba(255, 255, 255, 0.05);
                        backdrop-filter: blur(25px);
                        -webkit-backdrop-filter: blur(25px);
                        text-align: center;
                        z-index: 2;
                        animation: loadCard 0.6s cubic-bezier(0.16, 1, 0.3, 1);
                    }
                    /* Darkened/Sovereign Russian Flag representation */
                    .flag-stripes {
                        display: flex;
                        flex-direction: column;
                        width: 110px;
                        height: 20px;
                        margin: 0 auto 28px auto;
                        border-radius: 4px;
                        overflow: hidden;
                        opacity: 0.45;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
                        border: 1px solid rgba(255, 255, 255, 0.08);
                    }
                    .stripe {
                        flex: 1;
                        width: 100%;
                    }
                    .stripe-w { background-color: #E2E8F0; }
                    .stripe-b { background-color: #1E3A8A; }
                    .stripe-r { background-color: #991B1B; }

                    /* Summer Organic Decorative Leaf Tag */
                    .tag {
                        display: inline-flex;
                        align-items: center;
                        gap: 6px;
                        color: #10B981;
                        background: rgba(16, 185, 129, 0.09);
                        border: 1px solid rgba(16, 185, 129, 0.2);
                        font-weight: 700;
                        font-size: 11px;
                        letter-spacing: 1px;
                        padding: 7px 15px;
                        border-radius: 100px;
                        margin-bottom: 24px;
                        text-transform: uppercase;
                    }
                    h1 {
                        font-size: 22px;
                        color: #FFFFFF;
                        margin: 0 0 16px 0;
                        letter-spacing: 1.5px;
                        font-weight: 900;
                        text-transform: uppercase;
                    }
                    .desc {
                        font-size: 14px;
                        color: #94A3B8;
                        line-height: 1.6;
                        margin-bottom: 28px;
                    }
                    .alert-info {
                        background: rgba(2, 6, 23, 0.5);
                        border-left: 4px solid #EF4444;
                        padding: 18px;
                        border-radius: 12px;
                        text-align: left;
                        margin-bottom: 30px;
                        border-top: 1px solid rgba(255, 255, 255, 0.02);
                        border-bottom: 1px solid rgba(255, 255, 255, 0.02);
                    }
                    .info-row {
                        margin-bottom: 10px;
                    }
                    .info-row:last-child {
                        margin-bottom: 0;
                    }
                    .info-lbl {
                        color: #64748B;
                        font-weight: 700;
                        display: block;
                        margin-bottom: 4px;
                        text-transform: uppercase;
                        font-size: 10px;
                        letter-spacing: 1px;
                    }
                    .info-val {
                        color: #F1F5F9;
                        word-break: break-all;
                        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                        font-size: 13px;
                    }
                    .law-link {
                        color: #38BDF8;
                        text-decoration: none;
                        font-weight: 600;
                        transition: opacity 0.2s;
                        border-bottom: 1px dashed rgba(56, 189, 248, 0.4);
                        padding-bottom: 1px;
                    }
                    .law-link:hover {
                        opacity: 0.85;
                        color: #7DD3FC;
                    }
                    .btn-group {
                        display: flex;
                        gap: 14px;
                        justify-content: center;
                    }
                    .btn {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        background: #1E293B;
                        border: 1px solid rgba(255, 255, 255, 0.1);
                        color: #FFFFFF;
                        text-decoration: none;
                        padding: 12px 32px;
                        border-radius: 12px;
                        font-weight: 700;
                        font-size: 14px;
                        cursor: pointer;
                        transition: all 0.2s;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.25);
                    }
                    .btn:hover {
                        transform: translateY(-2px);
                        background: #334155;
                        border-color: rgba(255, 255, 255, 0.2);
                    }
                    .btn:active {
                        transform: translateY(0);
                    }
                    .btn-primary {
                        background: linear-gradient(135deg, #059669 0%, #10B981 100%);
                        border: none;
                        box-shadow: 0 4px 15px rgba(16, 185, 129, 0.25);
                    }
                    .btn-primary:hover {
                        background: linear-gradient(135deg, #047857 0%, #059669 100%);
                        box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
                    }
                    .footer {
                        margin-top: 35px;
                        font-size: 10px;
                        color: #475569;
                        letter-spacing: 1px;
                        font-weight: 700;
                        text-transform: uppercase;
                    }
                    @keyframes loadCard {
                        from { opacity: 0; transform: translateY(20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                </style>
            </head>
            <body>
                <div class="sun-glow"></div>
                <div class="meadow-glow"></div>
                
                <div class="card">
                    <!-- Sovereign Darkened Tricolor Flag -->
                    <div class="flag-stripes">
                        <div class="stripe stripe-w"></div>
                        <div class="stripe stripe-b"></div>
                        <div class="stripe stripe-r"></div>
                    </div>
                    
                    <div class="tag">🌱 Летний Щит — РФ</div>
                    <h1>ДОСТУП ОГРАНИЧЕН</h1>
                    <p class="desc">Данный интернет-ресурс заблокирован в связи с нарушениями требований законодательства РФ. Навигация к нему приостановлена.</p>
                    
                    <div class="alert-info">
                        <div class="info-row">
                            <span class="info-lbl">Адрес сайта:</span>
                            <span class="info-val" style="color: #FCA5A5;">$url</span>
                        </div>
                        <div class="info-row">
                            <span class="info-lbl">Основание ограничения:</span>
                            <span class="info-val">
                                Соответствие требованиям <a href="http://www.consultant.ru/document/cons_doc_LAW_61798/" target="_blank" class="law-link">Закона 149-ФЗ</a> "Об информации, информационных технологиях и о защите информации"
                            </span>
                        </div>
                    </div>

                    <div class="btn-group">
                        <a onclick="window.history.back();" class="btn btn-primary">Назад</a>
                        <a href="about:home" class="btn">Домой</a>
                    </div>
                    
                    <div class="footer">
                        Суверенный щит безопасности • Росбраузер
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
