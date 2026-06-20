package com.example.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Filter levels of the local sovereign DNS / web firewall.
 */
enum class FilteringLevel(val displayName: String, val description: String) {
    LOW("Базовый", "Проверка угроз отключена. Полный свободный доступ к ресурсам без фильтрации."),
    MEDIUM("Стандартный", "Блокировка фишинга, агрессивной рекламы и шпионских трекеров (метрики, телеметрия)."),
    HIGH("Суверенный", "Максимальная защита. Фильтрация на основе реестра запрещенных сайтов РКН и вредоносных ресурсов.")
}

/**
 * Service block rules
 */
data class BlockedService(
    val name: String,
    val domain: String,
    val isSystem: Boolean = false,
    var isEnabled: Boolean = true
)

object RknBlocklistManager {
    // Current filtering level
    private val _filteringLevel = MutableStateFlow(FilteringLevel.HIGH)
    val filteringLevel: StateFlow<FilteringLevel> = _filteringLevel.asStateFlow()

    // Real-time counter of blocked domains
    private val _blockedDomainsCount = MutableStateFlow(42) // Start with an initial nice seed
    val blockedDomainsCount: StateFlow<Int> = _blockedDomainsCount.asStateFlow()

    // Custom blocked domains added by user
    private val _customBlockedDomains = MutableStateFlow(
        setOf("custom-spyware.ru", "ad-tracker-malicious.com")
    )
    val customBlockedDomains: StateFlow<Set<String>> = _customBlockedDomains.asStateFlow()

    // Service-level blocking presets
    private val _blockedServices = MutableStateFlow(
        listOf(
            BlockedService("Крипто-майнеры", "coin-miner.org", isSystem = true),
            BlockedService("Шпионское ПО", "spy-agent.net", isSystem = true),
            BlockedService("Рекламные сети", "doubleclick.net", isSystem = true),
            BlockedService("Facebook", "facebook.com", isSystem = false),
            BlockedService("Instagram", "instagram.com", isSystem = false),
            BlockedService("X (Twitter)", "twitter.com", isSystem = false),
            BlockedService("LinkedIn", "linkedin.com", isSystem = false)
        )
    )
    val blockedServices: StateFlow<List<BlockedService>> = _blockedServices.asStateFlow()

    // Hardcoded demo list representing some typical registry bans
    private val roskomnadzorRegistry = setOf(
        "fb.com", "facebook.com", "instagram.com", "twitter.com", "x.com", "linkedin.com", 
        "t.co", "flibusta.is", "rutracker.org", "banned-site.ru", "phishing-bank.ru"
    )

    private val adAndTrackerRegistry = setOf(
        "doubleclick.net", "google-analytics.com", "yandex.ru/clck", "telemetry.net", "ads.com"
    )

    fun setFilteringLevel(level: FilteringLevel) {
        _filteringLevel.value = level
    }

    /**
     * Checks if a navigation URL should be blocked based on the current configuration.
     * Increments the blocked domain counter if yes.
     */
    fun shouldBlock(url: String): Boolean {
        val level = _filteringLevel.value
        if (level == FilteringLevel.LOW) {
            return false // No firewall active
        }

        val host = extractHost(url)

        // 1. Check custom blocked domains
        if (_customBlockedDomains.value.any { host.contains(it, ignoreCase = true) }) {
            incrementBlockedCounter()
            return true
        }

        // 2. Check disabled service presets
        _blockedServices.value.forEach { service ->
            if (service.isEnabled && host.contains(service.domain, ignoreCase = true)) {
                incrementBlockedCounter()
                return true
            }
        }

        // 3. Medium filtering: Ads & standard tracker threats
        if (level == FilteringLevel.MEDIUM) {
            if (adAndTrackerRegistry.any { host.contains(it, ignoreCase = true) }) {
                incrementBlockedCounter()
                return true
            }
        }

        // 4. Sovereign level: All of above + standard RKN blocklist registry
        if (level == FilteringLevel.HIGH) {
            if (adAndTrackerRegistry.any { host.contains(it, ignoreCase = true) } ||
                roskomnadzorRegistry.any { host.contains(it, ignoreCase = true) }) {
                incrementBlockedCounter()
                return true
            }
        }

        return false
    }

    fun addCustomBlockedDomain(domain: String) {
        val cleanDomain = domain.trim().lowercase(Locale.ROOT)
        if (cleanDomain.isNotEmpty()) {
            _customBlockedDomains.value = _customBlockedDomains.value + cleanDomain
        }
    }

    fun removeCustomBlockedDomain(domain: String) {
        _customBlockedDomains.value = _customBlockedDomains.value - domain
    }

    fun toggleServiceBlock(name: String, enabled: Boolean) {
        _blockedServices.value = _blockedServices.value.map {
            if (it.name == name) it.copy(isEnabled = enabled) else it
        }
    }

    fun addCustomService(name: String, domain: String) {
        val cleanName = name.trim()
        val cleanDomain = domain.trim().lowercase(Locale.ROOT)
        if (cleanName.isNotEmpty() && cleanDomain.isNotEmpty()) {
            _blockedServices.value = _blockedServices.value + BlockedService(
                name = cleanName,
                domain = cleanDomain,
                isSystem = false,
                isEnabled = true
            )
        }
    }

    fun incrementBlockedCounter() {
        _blockedDomainsCount.value = _blockedDomainsCount.value + 1
    }

    private fun extractHost(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
