package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)


    // Surfing History
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteHistoryItemById(id: Int)

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()


    // Blocked URL List
    @Query("SELECT * FROM blocked_urls ORDER BY pattern ASC")
    fun getAllBlockedUrls(): Flow<List<BlockedUrl>>

    @Query("SELECT * FROM blocked_urls")
    suspend fun getBlockedUrlsListSync(): List<BlockedUrl>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUrl(blockedUrl: BlockedUrl)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUrls(blockedUrls: List<BlockedUrl>)

    @Query("DELETE FROM blocked_urls WHERE id = :id")
    suspend fun deleteBlockedUrlById(id: Int)

    @Query("DELETE FROM blocked_urls")
    suspend fun clearBlockedUrls()

    @Query("SELECT COUNT(*) FROM blocked_urls")
    suspend fun getBlockedUrlsCount(): Int


    // Blocked Attempts (Stats)
    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC")
    fun getAllBlockedAttempts(): Flow<List<BlockedAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedAttempt(attempt: BlockedAttempt)

    @Query("SELECT COUNT(*) FROM blocked_attempts")
    fun getBlockedAttemptsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_attempts")
    suspend fun getBlockedAttemptsCount(): Int

    @Query("DELETE FROM blocked_attempts")
    suspend fun clearBlockedAttempts()
}
