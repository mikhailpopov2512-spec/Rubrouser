package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [BlockedUrl::class, Bookmark::class, HistoryItem::class, BlockedAttempt::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {

    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "rosbrowser_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default blocklists on first create
                        INSTANCE?.let { database ->
                            scope.launch(Dispatchers.IO) {
                                populateDefaultBlockedUrls(database.browserDao())
                                populateDefaultBookmarks(database.browserDao())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultBlockedUrls(dao: BrowserDao) {
            val defaults = listOf(
                BlockedUrl(pattern = "facebook.com", reason = "Решение Генпрокуратуры РФ от 21.03.2022 по признанию деятельности корпорации Meta экстремистской"),
                BlockedUrl(pattern = "instagram.com", reason = "Решение Генпрокуратуры РФ от 21.03.2022 по признанию деятельности корпорации Meta экстремистской"),
                BlockedUrl(pattern = "twitter.com", reason = "Ограничение доступа в соответствии со статьей 15.3 ФЗ №149-ФЗ на основании требований Генеральной прокуратуры РФ"),
                BlockedUrl(pattern = "x.com", reason = "Ограничение доступа в соответствии со статьей 15.3 ФЗ №149-ФЗ на основании требований Генеральной прокуратуры РФ"),
                BlockedUrl(pattern = "blocked-example.com", reason = "Демонстрационный ресурс, внесённый в Единый реестр запрещённых сайтов (ФЗ-149)"),
                BlockedUrl(pattern = "unapproved-site.ru", reason = "Ресурс заблокирован Роскомнадзором за нарушение законодательства о персональных данных")
            )
            dao.insertBlockedUrls(defaults)
        }

        private suspend fun populateDefaultBookmarks(dao: BrowserDao) {
            val defaults = listOf(
                Bookmark(title = "Яндекс.Поиск", url = "https://ya.ru"),
                Bookmark(title = "Госуслуги", url = "https://www.gosuslugi.ru"),
                Bookmark(title = "ВКонтакте", url = "https://vk.com"),
                Bookmark(title = "Rutube", url = "https://rutube.ru")
            )
            for (b in defaults) {
                dao.insertBookmark(b)
            }
        }
    }
}
