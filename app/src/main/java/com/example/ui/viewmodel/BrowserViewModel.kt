package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PasswordRepository
import com.example.data.SavedCredential
import com.example.ui.components.BackgroundTheme
import com.example.utils.FilteringLevel
import com.example.utils.RknBlocklistManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BrowserTab {
    HOME,
    SETTINGS,
    PASSWORD_MANAGER,
    ABOUT
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val passwordRepository = PasswordRepository(application)

    // Current page URL
    private val _currentUrl = MutableStateFlow("rosbrowser://home")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    // Home visual search text
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // Screen tabs/views
    private val _activeTab = MutableStateFlow(BrowserTab.HOME)
    val activeTab: StateFlow<BrowserTab> = _activeTab.asStateFlow()

    // Visual theme setting
    private val _backgroundTheme = MutableStateFlow(BackgroundTheme.SUMMER)
    val backgroundTheme: StateFlow<BackgroundTheme> = _backgroundTheme.asStateFlow()

    // Password manager biometric / login state
    private val _isPasswordManagerUnlocked = MutableStateFlow(false)
    val isPasswordManagerUnlocked: StateFlow<Boolean> = _isPasswordManagerUnlocked.asStateFlow()

    // Account system
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // List of credentials from repository
    val credentialsList: StateFlow<List<SavedCredential>> = passwordRepository.credentials

    // Blocklist states linked to RknBlocklistManager singleton
    val filteringLevel = RknBlocklistManager.filteringLevel
    val blockedCount = RknBlocklistManager.blockedDomainsCount
    val customBlockedList = RknBlocklistManager.customBlockedDomains
    val blockedServices = RknBlocklistManager.blockedServices

    // Live logging of blocked activities for visual feedback on start page
    private val _blockedLogs = MutableStateFlow<List<String>>(emptyList())
    val blockedLogs: StateFlow<List<String>> = _blockedLogs.asStateFlow()

    init {
        // Shared preferences for basic user registration persistence
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("rosbrowser_user", Application.MODE_PRIVATE)
        _isRegistered.value = prefs.getBoolean("user_registered", false)
        _isLoggedIn.value = prefs.getBoolean("user_logged_in", false)
        _userName.value = prefs.getString("user_name", "") ?: ""
        _userEmail.value = prefs.getString("user_email", "") ?: ""

        val savedTheme = prefs.getString("user_theme", "SUMMER") ?: "SUMMER"
        _backgroundTheme.value = try {
            BackgroundTheme.valueOf(savedTheme)
        } catch (e: Exception) {
            BackgroundTheme.SUMMER
        }

        val savedLevel = prefs.getString("filtering_level", "HIGH") ?: "HIGH"
        try {
            RknBlocklistManager.setFilteringLevel(FilteringLevel.valueOf(savedLevel))
        } catch (e: Exception) {}

        // Mock some dynamic security inspection threats over time to make the dashboard alive
        startSecuritySnooperSimulator()
    }

    private fun startSecuritySnooperSimulator() {
        viewModelScope.launch {
            val simulationDomains = listOf(
                "metrica.yandex.ru/analytics",
                "google-analytics.com/j/collect",
                "doubleclick.net/ads",
                "ad.host.tracking.ru",
                "facebook.com/tr",
                "spy-telemetry.net/ping",
                "api.instagram.com/graphql"
            )
            var index = 0
            while (true) {
                delay(12000) // Every 12 seconds check/block dynamic tracking in background
                if (RknBlocklistManager.filteringLevel.value != FilteringLevel.LOW) {
                    val d = simulationDomains[index % simulationDomains.size]
                    RknBlocklistManager.incrementBlockedCounter()
                    addBlockedLog("Блокирован запрос трекера: $d")
                    index++
                }
            }
        }
    }

    private fun addBlockedLog(log: String) {
        val currentList = _blockedLogs.value.toMutableList()
        currentList.add(0, log)
        if (currentList.size > 8) currentList.removeAt(8)
        _blockedLogs.value = currentList
    }

    fun setUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty()) {
            if (RknBlocklistManager.shouldBlock(cleanUrl)) {
                _currentUrl.value = "rosbrowser://blocked?url=$cleanUrl"
            } else {
                _currentUrl.value = cleanUrl
            }
        }
    }

    fun submitSearch(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return
        
        // If it looks like a domain name, navigate. Otherwise, query Russian sovereign Search Engine
        if (cleanQuery.contains(".") && !cleanQuery.contains(" ")) {
            val targetUrl = if (cleanQuery.startsWith("http://") || cleanQuery.startsWith("https://")) {
                cleanQuery
            } else {
                "https://$cleanQuery"
            }
            setUrl(targetUrl)
        } else {
            // Yandex search as Russian sovereign default
            setUrl("https://ya.ru/search/?text=${android.net.Uri.encode(cleanQuery)}")
        }
    }

    fun goHome() {
        _currentUrl.value = "rosbrowser://home"
        _searchText.value = ""
    }

    fun selectTab(tab: BrowserTab) {
        _activeTab.value = tab
    }

    fun updateTheme(theme: BackgroundTheme) {
        _backgroundTheme.value = theme
        val prefs = getApplication<Application>().getSharedPreferences("rosbrowser_user", Application.MODE_PRIVATE)
        prefs.edit().putString("user_theme", theme.name).apply()
    }

    fun updateFilteringLevel(level: FilteringLevel) {
        RknBlocklistManager.setFilteringLevel(level)
        val prefs = getApplication<Application>().getSharedPreferences("rosbrowser_user", Application.MODE_PRIVATE)
        prefs.edit().putString("filtering_level", level.name).apply()
    }

    // Password Manager functions
    fun unlockPasswordManager(unlocked: Boolean) {
        _isPasswordManagerUnlocked.value = unlocked
    }

    fun saveCredential(service: String, user: String, pass: String) {
        viewModelScope.launch {
            passwordRepository.addCredential(
                SavedCredential(
                    serviceName = service,
                    username = user,
                    password = pass
                )
            )
        }
    }

    fun deleteCredential(id: String) {
        viewModelScope.launch {
            passwordRepository.deleteCredential(id)
        }
    }

    // Auth actions
    fun registerUser(name: String, email: String, onComplete: () -> Unit) {
        val prefs = getApplication<Application>().getSharedPreferences("rosbrowser_user", Application.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("user_registered", true)
            .putBoolean("user_logged_in", true)
            .putString("user_name", name)
            .putString("user_email", email)
            .apply()

        _isRegistered.value = true
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        onComplete()
    }

    fun logoutUser() {
        val prefs = getApplication<Application>().getSharedPreferences("rosbrowser_user", Application.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("user_logged_in", false)
            .apply()
        _isLoggedIn.value = false
        _isPasswordManagerUnlocked.value = false
    }

    fun loginUser(email: String, onComplete: () -> Unit) {
        if (_isRegistered.value && email.trim().lowercase() == _userEmail.value.lowercase()) {
            val prefs = getApplication<Application>().getSharedPreferences("rosbrowser_user", Application.MODE_PRIVATE)
            prefs.edit().putBoolean("user_logged_in", true).apply()
            _isLoggedIn.value = true
            onComplete()
        }
    }
}
