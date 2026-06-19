package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("rosbrowser_prefs", Context.MODE_PRIVATE)
    private val scope = viewModelScope

    // Database / Repository
    private val database = BrowserDatabase.getDatabase(application, scope)
    val repository = BrowserRepository(database.browserDao())

    // UI state parameters
    val bookmarks = repository.allBookmarks.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repository.allHistory.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blockedUrls = repository.allBlockedUrls.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blockedAttempts = repository.allBlockedAttempts.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blockedAttemptsCount = repository.blockedAttemptsCount.stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    // Current page state
    val currentUrl = MutableStateFlow("about:home")
    val pageTitle = MutableStateFlow("Новая вкладка")
    val isWebLoading = MutableStateFlow(false)
    val webProgress = MutableStateFlow(0)

    // Legal / Compliance accept state
    val hasAcceptedTerms = MutableStateFlow(sharedPrefs.getBoolean("accepted_terms", true))

    // Search Engine
    // 0 = Яндекс, 1 = Mail.ru, 2 = Rambler
    val selectedSearchEngine = MutableStateFlow(sharedPrefs.getInt("search_engine", 0))

    // DNS over HTTPS
    // 0 = Системный, 1 = Яндекс DoH, 2 = НИИ Восход DoH
    val selectedDnsType = MutableStateFlow(sharedPrefs.getInt("dns_type", 1))

    // Sync state
    // 0 = Отключена, 1 = Яндекс ID, 2 = VK ID, 3 = Свой сервер
    val syncType = MutableStateFlow(sharedPrefs.getInt("sync_type", 0))
    val customSyncEndpoint = MutableStateFlow(sharedPrefs.getString("sync_endpoint", "https://api.rosbrowser.ru/sync") ?: "https://api.rosbrowser.ru/sync")
    val syncAccountName = MutableStateFlow(sharedPrefs.getString("sync_account_name", "") ?: "")
    val isSyncing = MutableStateFlow(false)
    val syncStatusMessage = MutableStateFlow("Синхронизация отключена")

    // Dynamic Live notification state for 100% visual arrival guarantees
    val liveNotification = MutableStateFlow<Pair<String, String>?>(null)

    fun showLiveNotification(title: String, message: String) {
        liveNotification.value = Pair(title, message)
        try {
            com.example.utils.BrowserNotificationHelper.showNotification(
                getApplication(),
                id = (System.currentTimeMillis() % 10000 + 5000).toInt(),
                title = title,
                message = message
            )
        } catch (t: Throwable) {
            // Safe fallback
        }
    }

    // Security & AdBlock values
    val isSafeBrowsingEnabled = MutableStateFlow(sharedPrefs.getBoolean("safe_browsing", true))
    val isAdBlockEnabled = MutableStateFlow(sharedPrefs.getBoolean("ad_block", true))
    val isCheburcheckEnabled = MutableStateFlow(sharedPrefs.getBoolean("cheburcheck_enabled", true))

    // GOST Encryption suites: 0 = ГОСТ Р 34.12-2015 "Кузнечик" (Kuznyechik), 1 = ГОСТ 28147-89, 2 = ГОСТ Р 34.10-2012 / ГОСТ Р 34.11-2012
    val selectedGostCipherSuite = MutableStateFlow(sharedPrefs.getInt("gost_cipher_suite", 0))
    // Strict trust store: true = only national Russian government certificates allowed, false = standard web trust with national certificates enabled
    val useMintsifryCertsOnly = MutableStateFlow(sharedPrefs.getBoolean("mintsifry_certs_only", false))

    // Theme select: 0 = Системная, 1 = Светлая (по умолчанию), 2 = Тёмная
    val selectedThemeMode = MutableStateFlow(sharedPrefs.getInt("theme_mode", 1))

    // NTP Background Select: 0 = Летний пейзаж (По умолчанию), 1 = Российский флаг (Патриотический), 2 = Минималистичный (Чистый цвет)
    val selectedNtpThemeBackground = MutableStateFlow(sharedPrefs.getInt("ntp_bg_theme", 0))

    // Address Bar Position: 0 = Снизу (по умолчанию), 1 = Сверху
    val selectedAddressBarPosition = MutableStateFlow(sharedPrefs.getInt("address_bar_pos", 0))

    // Browser Modes: 0 = Стандартный, 1 = Инкогнито, 2 = Гостевой, 3 = Детский, 4 = Stealth (Скрытный)
    val currentBrowserMode = MutableStateFlow(0)

    // NTP Widgets Display Toggles
    val showWeatherWidget = MutableStateFlow(sharedPrefs.getBoolean("w_weather", true))
    val showTrafficWidget = MutableStateFlow(sharedPrefs.getBoolean("w_traffic", true))
    val showRatesWidget = MutableStateFlow(sharedPrefs.getBoolean("w_rates", true))
    val showDzenWidget = MutableStateFlow(sharedPrefs.getBoolean("w_dzen", true))

    // RKN database info
    val lastRknUpdate = MutableStateFlow(sharedPrefs.getString("rkn_last_update", "13.06.2026 04:12") ?: "13.06.2026 04:12")
    val isUpdatingRknList = MutableStateFlow(false)

    init {
        if (!sharedPrefs.getBoolean("accepted_terms", false)) {
            sharedPrefs.edit().putBoolean("accepted_terms", true).apply()
        }
        initializeBrowserData()
    }

    fun initializeBrowserData() {
        // Log sync state on start
        updateSyncMessage()

        // Asynchronously check and prepopulate database if empty on Dispatchers.IO, avoiding Room onCreate deadlocks
        scope.launch(Dispatchers.IO) {
            try {
                if (repository.getBlockedUrlsCount() == 0) {
                    repository.restoreDefaultBlocklist()
                }
                if (repository.getBookmarksCount() == 0) {
                    repository.restoreDefaultBookmarks()
                }
            } catch (e: Exception) {
                android.util.Log.e("BrowserViewModel", "Error prepopulating Database", e)
            }
        }
    }

    // Theme and Bar Position setters
    fun setThemeMode(mode: Int) {
        sharedPrefs.edit().putInt("theme_mode", mode).apply()
        selectedThemeMode.value = mode
    }

    fun setNtpBackgroundTheme(theme: Int) {
        sharedPrefs.edit().putInt("ntp_bg_theme", theme).apply()
        selectedNtpThemeBackground.value = theme
    }

    fun setAddressBarPosition(pos: Int) {
        sharedPrefs.edit().putInt("address_bar_pos", pos).apply()
        selectedAddressBarPosition.value = pos
    }

    fun setBrowserMode(mode: Int) {
        currentBrowserMode.value = mode
    }

    fun toggleWidget(widgetType: String, enabled: Boolean) {
        sharedPrefs.edit().putBoolean("w_$widgetType", enabled).apply()
        when (widgetType) {
            "weather" -> showWeatherWidget.value = enabled
            "traffic" -> showTrafficWidget.value = enabled
            "rates" -> showRatesWidget.value = enabled
            "dzen" -> showDzenWidget.value = enabled
        }
    }

    // Consent management
    fun acceptTerms() {
        sharedPrefs.edit().putBoolean("accepted_terms", true).apply()
        hasAcceptedTerms.value = true
        initializeBrowserData()
    }

    // Settings actions
    fun setSearchEngine(id: Int) {
        sharedPrefs.edit().putInt("search_engine", id).apply()
        selectedSearchEngine.value = id
    }

    fun setDnsType(id: Int) {
        sharedPrefs.edit().putInt("dns_type", id).apply()
        selectedDnsType.value = id
    }

    fun toggleSafeBrowsing(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("safe_browsing", enabled).apply()
        isSafeBrowsingEnabled.value = enabled
    }

    fun toggleAdBlock(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("ad_block", enabled).apply()
        isAdBlockEnabled.value = enabled
    }

    fun toggleCheburcheck(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("cheburcheck_enabled", enabled).apply()
        isCheburcheckEnabled.value = enabled
    }

    fun setGostCipherSuite(id: Int) {
        sharedPrefs.edit().putInt("gost_cipher_suite", id).apply()
        selectedGostCipherSuite.value = id
        showLiveNotification(
            title = "Конфигурация ГОСТ изменена",
            message = "Выбран новый стандарт шифрования: " + when(id) {
                0 -> "ГОСТ Р 34.12-2015 'Кузнечик'"
                1 -> "ГОСТ 28147-89"
                else -> "ГОСТ Р 34.10-2012 / 34.11-2012"
            }
        )
    }

    fun toggleMintsifryCertsOnly(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("mintsifry_certs_only", enabled).apply()
        useMintsifryCertsOnly.value = enabled
        showLiveNotification(
            title = if (enabled) "Строгое доверие Минцифры" else "Гибридный режим доверия",
            message = if (enabled) "Соединения разрешены ТОЛЬКО к отечественным узлам с ГОСТ-сертификацией." else "Разрешены все легитимные соединения с поддержкой национальных ГОСТ-сертификатов."
        )
    }

    // Manual blocklist checking
    fun updateRknBlocklist() {
        viewModelScope.launch {
            isUpdatingRknList.value = true
            // Simulate 1.5s update from https://api.rkn.gov.ru/
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(1500)
                // Add an update domain randomly to show action
                repository.addBlockedUrl("banned-site-${System.currentTimeMillis() % 1000}.ru", "Решение Роскомнадзора об ограничении доступа на основании Федерального закона №149-ФЗ")
            }
            val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())
            val nowStr = formatter.format(java.util.Date())
            sharedPrefs.edit().putString("rkn_last_update", nowStr).apply()
            lastRknUpdate.value = nowStr
            isUpdatingRknList.value = false
            showLiveNotification(
                title = "Реестр РКН обновлен",
                message = "База ограничений успешно синхронизирована вручную: $nowStr"
            )
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            repository.clearBlockedAttempts()
        }
    }

    fun addBlockedUrl(pattern: String, reason: String) {
        viewModelScope.launch {
            repository.addBlockedUrl(pattern.lowercase().trim(), reason.ifBlank { "Решение Роскомнадзора об ограничении доступа к сайту" })
        }
    }

    fun deleteBlockedUrl(id: Int) {
        viewModelScope.launch {
            repository.deleteBlockedUrl(id)
        }
    }

    fun restoreBlocklistDefaults() {
        viewModelScope.launch {
            isUpdatingRknList.value = true
            withContext(Dispatchers.IO) {
                repository.restoreDefaultBlocklist()
                kotlinx.coroutines.delay(800)
            }
            isUpdatingRknList.value = false
        }
    }

    // Navigation and core loading
    val searchSuggestionQuery = MutableStateFlow("")
    val searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val localHistorySuggestions = MutableStateFlow<List<com.example.data.database.HistoryItem>>(emptyList())
    val localBookmarkSuggestions = MutableStateFlow<List<com.example.data.database.Bookmark>>(emptyList())

    private var searchSuggestJob: kotlinx.coroutines.Job? = null
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Non-blocking cancellable coroutine wrapper for OkHttp
    private suspend fun okhttp3.Call.await(): okhttp3.Response = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (e: Exception) {
                // Ignore
            }
        }
        enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(e))
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (continuation.isActive) {
                    continuation.resume(response) { _, _, _ -> }
                } else {
                    try {
                        response.close()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        })
    }

    fun onSearchInputChanged(query: String) {
        searchSuggestionQuery.value = query
        searchSuggestJob?.cancel()
        
        if (query.isBlank()) {
            searchSuggestions.value = emptyList()
            localHistorySuggestions.value = emptyList()
            localBookmarkSuggestions.value = emptyList()
            return
        }

        // Fetch local and remote suggestions with a solid 300ms debounce
        searchSuggestJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            
            // Instantly fetch from thread-safe memory cached lists, avoiding any DB locks/waits!
            try {
                val historyList = repository.getCachedHistory()
                val matchedHistory = historyList.filter {
                    it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
                }.take(3)

                val bookmarkList = repository.getCachedBookmarks()
                val matchedBookmarks = bookmarkList.filter {
                    it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
                }.take(3)
                
                localHistorySuggestions.value = matchedHistory
                localBookmarkSuggestions.value = matchedBookmarks
            } catch (e: Exception) {
                android.util.Log.e("BrowserViewModel", "Error matching local suggestions in-memory", e)
            }
            
            fetchSuggestions(query)
        }
    }

    private suspend fun fetchSuggestions(query: String) {
        withContext(Dispatchers.IO) {
            var attempt = 0
            val maxRetries = 2
            var delayMs = 150L
            var success = false
            
            while (attempt < maxRetries && !success) {
                // Immediately check for coroutine active status using explicit Job context to prevent conflicts
                val job = kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]
                if (job?.isActive != true) {
                    throw kotlinx.coroutines.CancellationException("Search cancelled")
                }
                try {
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    val url = "https://suggest.yandex.ru/suggest-ya.cgi?v=4&part=$encoded"
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    val call = httpClient.newCall(request)
                    val response = call.await()
                    response.use { res ->
                        if (res.isSuccessful) {
                            val body = res.body?.string() ?: ""
                            val isCtxActive = kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.isActive == true
                            if (body.isNotBlank() && isCtxActive) {
                                val jsonArray = org.json.JSONArray(body)
                                if (jsonArray.length() > 1) {
                                    val suggestionsArray = jsonArray.getJSONArray(1)
                                    val results = mutableListOf<String>()
                                    for (i in 0 until suggestionsArray.length()) {
                                        val s = suggestionsArray.optString(i)
                                        if (s.isNotBlank()) {
                                            results.add(s)
                                        }
                                    }
                                    if (kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                                        withContext(Dispatchers.Main) {
                                            searchSuggestions.value = results.take(8)
                                        }
                                    }
                                    success = true
                                }
                            }
                        } else {
                            android.util.Log.e("BrowserViewModel", "HTTP response is not successful: ${res.code}")
                        }
                    }
                    if (!success) {
                        attempt++
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(delayMs)
                            delayMs *= 2
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BrowserViewModel", "Error fetching search suggestions (attempt ${attempt + 1})", e)
                    attempt++
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(delayMs)
                        delayMs *= 2
                    }
                }
            }
        }
    }

    fun getLayoutCorrection(input: String): String {
        val enToRu = mapOf(
            'q' to 'й', 'w' to 'ц', 'e' to 'у', 'r' to 'к', 't' to 'е', 'y' to 'н', 'u' to 'г', 'i' to 'ш', 'o' to 'щ', 'p' to 'з', '[' to 'х', ']' to 'ъ',
            'a' to 'ф', 's' to 'ы', 'd' to 'в', 'f' to 'а', 'g' to 'п', 'h' to 'р', 'j' to 'о', 'k' to 'л', 'l' to 'д', ';' to 'ж', '\'' to 'э',
            'z' to 'я', 'x' to 'ч', 'c' to 'с', 'v' to 'м', 'b' to 'и', 'n' to 'т', 'm' to 'ь', ',' to 'б', '.' to 'ю', '/' to '.'
        )
        val ruToEn = enToRu.entries.associate { (k, v) -> v to k }
        if (input.isBlank()) return input
        val ruConverted = input.map { enToRu[it.lowercaseChar()] ?: it }.joinToString("")
        val enConverted = input.map { ruToEn[it.lowercaseChar()] ?: it }.joinToString("")
        return if (ruConverted != input) ruConverted else enConverted
    }

    fun loadUrl(url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        if (cleanUrl == "chrome-native://blocked" || cleanUrl == "chrome://blocked") {
            currentUrl.value = "file:///android_asset/blocked.html"
            pageTitle.value = "Ресурс заблокирован"
            return
        }

        if (cleanUrl == "about:home") {
            currentUrl.value = "about:home"
            pageTitle.value = "Новая вкладка"
            return
        }

        // Improved browser heuristic: URL vs Search
        val isExplicitSearch = cleanUrl.startsWith("?")
        var targetUrl = cleanUrl
        
        val isSearch = if (isExplicitSearch) {
            targetUrl = cleanUrl.substring(1).trim()
            true
        } else {
            val hasSpace = cleanUrl.contains(" ")
            val hasDot = cleanUrl.contains(".")
            val isCommonProtocol = cleanUrl.startsWith("http://", ignoreCase = true) || 
                                   cleanUrl.startsWith("https://", ignoreCase = true) ||
                                   cleanUrl.startsWith("file://", ignoreCase = true) ||
                                   cleanUrl.startsWith("about:", ignoreCase = true)
            
            if (isCommonProtocol) {
                false
            } else if (hasSpace) {
                true
            } else if (!hasDot) {
                cleanUrl != "localhost"
            } else {
                // Determine if it looks like a valid domain or IP address
                val lastDotIndex = cleanUrl.lastIndexOf('.')
                val tld = cleanUrl.substring(lastDotIndex + 1)
                val isValidTld = tld.isNotEmpty() && tld.all { it.isLetter() } && tld.length in 2..6
                
                val isIpAddress = try {
                    val parts = cleanUrl.split('.')
                    parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
                } catch (e: Exception) {
                    false
                }
                
                !(isValidTld || isIpAddress)
            }
        }

        if (isSearch) {
            val query = java.net.URLEncoder.encode(targetUrl, "UTF-8")
            cleanUrl = getSearchEngineUrl(query)
        } else {
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://") && !cleanUrl.startsWith("about:")) {
                cleanUrl = "https://$cleanUrl"
            }
        }
        currentUrl.value = cleanUrl
    }

    private fun getSearchEngineUrl(encodedQuery: String): String {
        return when (selectedSearchEngine.value) {
            0 -> "https://ya.ru/search/?text=$encodedQuery"
            1 -> "https://go.mail.ru/search?q=$encodedQuery"
            2 -> "https://nova.rambler.ru/search?query=$encodedQuery"
            else -> "https://ya.ru/search/?text=$encodedQuery"
        }
    }

    // History and bookmarks additions
    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.insertBookmark(title, url)
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            repository.deleteBookmarkByUrl(url)
        }
    }

    fun addHistory(title: String, url: String) {
        if (url.startsWith("about:") || url.contains("blocked_stub")) return
        viewModelScope.launch {
            repository.addHistoryItem(title, url)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    // Sync Simulation
    fun setSyncType(type: Int, accountName: String = "", endpoint: String = "") {
        viewModelScope.launch {
            isSyncing.value = true
            sharedPrefs.edit().putInt("sync_type", type).apply()
            syncType.value = type

            if (accountName.isNotEmpty()) {
                sharedPrefs.edit().putString("sync_account_name", accountName).apply()
                syncAccountName.value = accountName
            }
            if (endpoint.isNotEmpty()) {
                sharedPrefs.edit().putString("sync_endpoint", endpoint).apply()
                customSyncEndpoint.value = endpoint
            }

            kotlinx.coroutines.delay(1200) // simulated network lag
            updateSyncMessage()
            isSyncing.value = false
        }
    }

    private fun updateSyncMessage() {
        syncStatusMessage.value = when (syncType.value) {
            0 -> "Синхронизация отключена"
            1 -> "Синхронизировано через Яндекс ID (${syncAccountName.value.ifBlank { "Пользователь" }})"
            2 -> "Синхронизировано через VK ID (${syncAccountName.value.ifBlank { "Пользователь" }})"
            3 -> "Синхронизировано через сервер: ${customSyncEndpoint.value}"
            else -> "Синхронизация отключена"
        }
    }
}
