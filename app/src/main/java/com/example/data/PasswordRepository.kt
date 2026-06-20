package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SavedCredential(
    val id: String = UUID.randomUUID().toString(),
    val serviceName: String,
    val username: String,
    val password: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

class PasswordRepository(context: Context) {
    private val prefs = context.getSharedPreferences("rosbrowser_passwords", Context.MODE_PRIVATE)

    private val _credentials = MutableStateFlow<List<SavedCredential>>(emptyList())
    val credentials: StateFlow<List<SavedCredential>> = _credentials.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        val serializedStr = prefs.getString("credentials_list_simple", null)
        if (serializedStr != null) {
            try {
                // Decode using reliable, simple double-slash and double-pipe splits
                val list = mutableListOf<SavedCredential>()
                val items = serializedStr.split("[ITEM_SEP]")
                for (item in items) {
                    val fields = item.split("[FIELD_SEP]")
                    if (fields.size >= 5) {
                        list.add(
                            SavedCredential(
                                id = fields[0],
                                serviceName = fields[1],
                                username = fields[2],
                                password = fields[3],
                                lastUpdated = fields[4].toLongOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
                _credentials.value = list
            } catch (e: Exception) {
                _credentials.value = emptyList()
            }
        } else {
            // Seed default credentials for a richer initial experience
            val defaultSeeds = listOf(
                SavedCredential(
                    serviceName = "Госуслуги РФ (gosuslugi.ru)",
                    username = "+7 999 123-45-67",
                    password = "• • • • • • • •"
                ),
                SavedCredential(
                    serviceName = "ВКонтакте (vk.com)",
                    username = "ivan_petrov",
                    password = "• • • • • • • •"
                ),
                SavedCredential(
                    serviceName = "Яндекс ID (yandex.ru)",
                    username = "ivan.petrov@yandex.ru",
                    password = "• • • • • • • •"
                )
            )
            _credentials.value = defaultSeeds
            saveToPrefs(defaultSeeds)
        }
    }

    fun addCredential(credential: SavedCredential) {
        val currentList = _credentials.value.toMutableList()
        currentList.add(0, credential) // Add at top
        _credentials.value = currentList
        saveToPrefs(currentList)
    }

    fun deleteCredential(id: String) {
        val updated = _credentials.value.filter { it.id != id }
        _credentials.value = updated
        saveToPrefs(updated)
    }

    private fun saveToPrefs(list: List<SavedCredential>) {
        val builder = java.lang.StringBuilder()
        list.forEachIndexed { index, cred ->
            builder.append(cred.id).append("[FIELD_SEP]")
                   .append(cred.serviceName).append("[FIELD_SEP]")
                   .append(cred.username).append("[FIELD_SEP]")
                   .append(cred.password).append("[FIELD_SEP]")
                   .append(cred.lastUpdated)
            if (index < list.size - 1) {
                builder.append("[ITEM_SEP]")
            }
        }
        prefs.edit().putString("credentials_list_simple", builder.toString()).apply()
    }
}
