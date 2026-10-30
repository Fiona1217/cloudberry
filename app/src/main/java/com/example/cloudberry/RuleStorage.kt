package com.example.cloudberry

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private data class StoredPlaylist(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null
)

private data class StoredTimeRange(
    val start: Int = 0,
    val end: Int = 0
)

private data class StoredRule(
    val id: String = "",
    val name: String = "",
    val playlists: List<StoredPlaylist> = emptyList(),
    val playlistOrder: List<StoredPlaylist> = emptyList(),
    val randomOrder: Boolean = false,
    val triggerOption: String = "",
    val season: String = "",
    val rainEnabled: Boolean = false,
    val rainScope: String = "season",
    val timeRanges: List<StoredTimeRange> = emptyList()
)

class RuleStorage {
    private val gson = Gson()
    private fun SpotifyPlaylist.toStoredPlaylist() =
        StoredPlaylist(
            id = id,
            name = name,
            imageUrl = imageUrl
        )
    private fun StoredPlaylist.toPlaylist() =
        SpotifyPlaylist(
            id = id,
            name = name,
            imageUrl = imageUrl
        )
    suspend fun loadRulesFromBackend(accessToken: String, backendBaseUrl: String): List<PlaylistRule> = withContext(Dispatchers.IO) {
        val connection = URL("$backendBaseUrl/api/rules").openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val status = connection.responseCode
            val response = (
                if (status in 200..299) connection.inputStream
                else connection.errorStream
                    )?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw Exception("Loading rules failed: HTTP $status — $response")
            }

            val responseObject = gson.fromJson(response, com.google.gson.JsonObject::class.java)
            val rulesJson = responseObject.getAsJsonArray("rules") ?: throw Exception("Backend response is missing 'rules'")
            val type = object : com.google.gson.reflect.TypeToken<List<StoredRule>>() {}.type
            val storedRules: List<StoredRule> = gson.fromJson(rulesJson, type)

            storedRules.map { stored ->
                PlaylistRule(
                    id = stored.id,
                    name = stored.name,
                    playlists = stored.playlists.map { it.toPlaylist() },
                    playlistOrder = stored.playlistOrder.map { it.toPlaylist() },
                    randomOrder = stored.randomOrder,
                    triggerOption = stored.triggerOption,
                    season = stored.season,
                    rainEnabled = stored.rainEnabled,
                    rainScope = stored.rainScope,
                    timeRanges = stored.timeRanges.map { it.start to it.end }
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun saveRulesToBackend(accessToken: String, backendBaseUrl: String, rules: List<PlaylistRule>) = withContext(Dispatchers.IO) {
        val storedRules = rules.map { rule ->
            StoredRule(
                id = rule.id,
                name = rule.name,
                playlists = rule.playlists.map { it.toStoredPlaylist() },
                playlistOrder = rule.playlistOrder.map { it.toStoredPlaylist() },
                randomOrder = rule.randomOrder,
                triggerOption = rule.triggerOption,
                season = rule.season,
                rainEnabled = rule.rainEnabled,
                rainScope = rule.rainScope,
                timeRanges = rule.timeRanges.map { StoredTimeRange(it.first, it.second) }
            )
        }

        val payload = gson.toJson(mapOf("rules" to storedRules))
        val connection = URL("$backendBaseUrl/api/rules").openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.outputStream.bufferedWriter().use { writer -> writer.write(payload) }

            val status = connection.responseCode
            val response = (
                if (status in 200..299) connection.inputStream
                else connection.errorStream
                    )?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw Exception("Saving rules failed: HTTP $status — $response")
            }
        } finally {
            connection.disconnect()
        }
    }
}
