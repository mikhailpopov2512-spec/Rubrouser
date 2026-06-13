package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "blocked_urls")
data class BlockedUrl(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String, // e.g. "instagram.com", "facebook.com", "blocked-example.com"
    val reason: String = "Ресурс заблокирован в соответствии со статьями 15.1, 15.3 Федерального закона № 149-ФЗ",
    val law: String = "ФЗ-149 от 27.07.2006",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "blocked_attempts")
data class BlockedAttempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
