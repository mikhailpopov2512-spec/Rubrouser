package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.URL

class BrowserRepository(private val browserDao: BrowserDao) {

    val allBookmarks: Flow<List<Bookmark>> = browserDao.getAllBookmarks()
    val allHistory: Flow<List<HistoryItem>> = browserDao.getAllHistory()
    val allBlockedUrls: Flow<List<BlockedUrl>> = browserDao.getAllBlockedUrls()
    val allBlockedAttempts: Flow<List<BlockedAttempt>> = browserDao.getAllBlockedAttempts()
    val blockedAttemptsCount: Flow<Int> = browserDao.getBlockedAttemptsCountFlow()

    // Bookmarks management
    suspend fun insertBookmark(title: String, url: String) = withContext(Dispatchers.IO) {
        browserDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun deleteBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        browserDao.deleteBookmark(bookmark)
    }

    suspend fun isBookmarked(url: String): Boolean = withContext(Dispatchers.IO) {
        browserDao.isBookmarked(url)
    }

    suspend fun deleteBookmarkByUrl(url: String) = withContext(Dispatchers.IO) {
        browserDao.deleteBookmarkByUrl(url)
    }

    suspend fun getBookmarksCount(): Int = withContext(Dispatchers.IO) {
        browserDao.getBookmarksCount()
    }

    suspend fun restoreDefaultBookmarks() = withContext(Dispatchers.IO) {
        val defaults = listOf(
            Bookmark(title = "Яндекс.Поиск", url = "https://ya.ru"),
            Bookmark(title = "Госуслуги", url = "https://www.gosuslugi.ru"),
            Bookmark(title = "ВКонтакте", url = "https://vk.com"),
            Bookmark(title = "Rutube", url = "https://rutube.ru")
        )
        for (b in defaults) {
            browserDao.insertBookmark(b)
        }
    }

    // History management
    suspend fun addHistoryItem(title: String, url: String) = withContext(Dispatchers.IO) {
        // Prevent empty names
        val cleanTitle = title.ifBlank { url }
        browserDao.insertHistoryItem(HistoryItem(title = cleanTitle, url = url))
    }

    suspend fun deleteHistoryItem(id: Int) = withContext(Dispatchers.IO) {
        browserDao.deleteHistoryItemById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        browserDao.clearHistory()
    }

    // Blocked URLs management
    suspend fun addBlockedUrl(pattern: String, reason: String) = withContext(Dispatchers.IO) {
        browserDao.insertBlockedUrl(BlockedUrl(pattern = pattern, reason = reason))
    }

    suspend fun deleteBlockedUrl(id: Int) = withContext(Dispatchers.IO) {
        browserDao.deleteBlockedUrlById(id)
    }

    suspend fun clearBlockedUrls() = withContext(Dispatchers.IO) {
        browserDao.clearBlockedUrls()
    }

    suspend fun getBlockedUrlsCount(): Int = withContext(Dispatchers.IO) {
        browserDao.getBlockedUrlsCount()
    }

    // Checking if a url is prohibited by RKN. Returns the BlockedUrl model if blocked, otherwise null.
    suspend fun checkBlockedUrl(urlString: String): BlockedUrl? = withContext(Dispatchers.IO) {
        val patterns = try {
            browserDao.getBlockedUrlsListSync()
        } catch (e: Exception) {
            emptyList()
        }
        val host = try {
            val cleanStr = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "http://$urlString"
            } else {
                urlString
            }
            URL(cleanStr).host?.lowercase() ?: ""
        } catch (e: Exception) {
            urlString.lowercase().trim()
        }

        patterns.find { blocked ->
            val pattern = blocked.pattern.lowercase().trim()
            host == pattern || host.endsWith(".$pattern")
        }
    }

    // Insert blocked attempt to log
    suspend fun logBlockedAttempt(url: String, reason: String) = withContext(Dispatchers.IO) {
        browserDao.insertBlockedAttempt(BlockedAttempt(url = url, reason = reason))
    }

    // Fetch block count
    suspend fun getBlockedCountSync(): Int = withContext(Dispatchers.IO) {
        browserDao.getBlockedAttemptsCount()
    }

    // Clear stats
    suspend fun clearBlockedAttempts() = withContext(Dispatchers.IO) {
        browserDao.clearBlockedAttempts()
    }

    // Re-fill default list (resets checklist to default)
    suspend fun restoreDefaultBlocklist() = withContext(Dispatchers.IO) {
        browserDao.clearBlockedUrls()
        val defaults = listOf(
            BlockedUrl(pattern = "facebook.com", reason = "Решение Генпрокуратуры РФ от 21.03.2022 по признанию деятельности корпорации Meta экстремистской"),
            BlockedUrl(pattern = "instagram.com", reason = "Решение Генпрокуратуры РФ от 21.03.2022 по признанию деятельности корпорации Meta экстремистской"),
            BlockedUrl(pattern = "twitter.com", reason = "Ограничение доступа в соответствии со статьей 15.3 ФЗ №149-ФЗ на основании требований Генеральной прокуратуры РФ"),
            BlockedUrl(pattern = "x.com", reason = "Ограничение доступа в соответствии со статьей 15.3 ФЗ №149-ФЗ на основании требований Генеральной прокуратуры РФ"),
            BlockedUrl(pattern = "blocked-example.com", reason = "Демонстрационный ресурс, внесёмный в Единый реестр запрещённых сайтов (ФЗ-149)"),
            BlockedUrl(pattern = "unapproved-site.ru", reason = "Ресурс заблокирован Роскомнадзором за нарушение законодательства о персональных данных")
        )
        browserDao.insertBlockedUrls(defaults)
    }
}
