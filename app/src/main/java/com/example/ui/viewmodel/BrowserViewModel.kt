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
    val hasAcceptedTerms = MutableStateFlow(sharedPrefs.getBoolean("accepted_terms", false))

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

    // Security & AdBlock values
    val isSafeBrowsingEnabled = MutableStateFlow(sharedPrefs.getBoolean("safe_browsing", true))
    val isAdBlockEnabled = MutableStateFlow(sharedPrefs.getBoolean("ad_block", true))

    // Theme select: 0 = Системная, 1 = Светлая (по умолчанию), 2 = Тёмная
    val selectedThemeMode = MutableStateFlow(sharedPrefs.getInt("theme_mode", 1))

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
        // Log sync state on start
        updateSyncMessage()
    }

    // Theme and Bar Position setters
    fun setThemeMode(mode: Int) {
        sharedPrefs.edit().putInt("theme_mode", mode).apply()
        selectedThemeMode.value = mode
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
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            repository.clearBlockedAttempts()
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
    fun loadUrl(url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        if (cleanUrl == "about:home") {
            currentUrl.value = "about:home"
            pageTitle.value = "Новая вкладка"
            return
        }

        // Search if not containing dots/slashes
        val isSearch = !cleanUrl.contains(".") || cleanUrl.contains(" ")
        if (isSearch) {
            val query = java.net.URLEncoder.encode(cleanUrl, "UTF-8")
            cleanUrl = getSearchEngineUrl(query)
        } else {
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
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
