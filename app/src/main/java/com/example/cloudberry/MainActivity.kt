package com.example.cloudberry

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cloudberry.ui.theme.CloudberryTheme
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.RectangleShape
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import java.util.Calendar

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)
class MainActivity : ComponentActivity() {

    // Keep the Client ID from your existing code.
    private val clientId = "06c5732c93554d8abaea5b478ea6d286"

    // Keep the existing redirect URI for App Remote.
    private val appRemoteRedirectUri =
        "https://com.example.cloudberry/callback"

    // Separate redirect URI for playlist authorization.
    private val authRedirectUri =
        "com.example.cloudberry://callback"

    private val authRequestCode = 1337

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var spotifyAccessToken: String? = null
    private var connectionStatus by mutableStateOf("Not connected")
    private var isConnecting by mutableStateOf(false)

    private var authStatus by mutableStateOf("Playlist access not authorized")
    private var isAuthorizing by mutableStateOf(false)
    private var isSpotifyLoggedIn by mutableStateOf(false)
    // Held temporarily until we add the backend token exchange.
    private var authorizationCode: String? = null
    private val backendBaseUrl = "http://10.77.208.109:8000"
    private var playlists by mutableStateOf<List<SpotifyPlaylist>>(emptyList())
    private var currentTrackName by mutableStateOf("")
    private var currentArtistName by mutableStateOf("")
    private var albumArtwork by mutableStateOf<Bitmap?>(null)
    private var isPlaying by mutableStateOf(false)
    private var hasCurrentTrack by mutableStateOf(false)
    private var currentPositionMs by mutableStateOf(0L)
    private var trackDurationMs by mutableStateOf(0L)
    private var playbackPositionUpdatedAt by mutableStateOf(0L)
    private var currentPlaylistName by mutableStateOf("")
    private var playlistStatus by mutableStateOf("Loading playlist...")
    private var isSwitchingPlaylist by mutableStateOf(false)
    private var selectedPage by mutableStateOf("home")
    private var spotifyProfilePicture by mutableStateOf<Bitmap?>(null)
    private var spotifyUsername by mutableStateOf("")
    private var lastCheckedTrackUri: String? = null
    private var counterPlaylistId: String? = null
    private var counterPlaylistName = ""
    private var currentTrackCounter = 0
    private var totalPlaylistTracks = 0
    private var pendingTrackDirection = 1
    private var counterLastTrackUri: String? = null
    private var counterLastTrackIndex = -1
    private var ignoreNextTrackChangeForCounter = false
    private var playlistTrackUris: List<String> = emptyList()
    @Volatile
    private var pendingPlaylist: SpotifyPlaylist? = null
    @Volatile
    private var isUpdatingPlaylist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpotifyAppRemote.setDebugMode(true)
        enableEdgeToEdge()

        setContent {
            CloudberryTheme {
                CloudberryHome(
                    connectionStatus = connectionStatus,
                    backendBaseUrl = backendBaseUrl,
                    spotifyAccessToken = spotifyAccessToken,
                    authStatus = authStatus,
                    isConnecting = isConnecting,
                    isAuthorizing = isAuthorizing,
                    isSpotifyLoggedIn = isSpotifyLoggedIn,
                    isSwitchingPlaylist = isSwitchingPlaylist,
                    onLogoutSpotify = { logoutSpotify() },
                    onLoginLogout = { handleSpotifyLoginLogout() },
                    spotifyProfilePicture = spotifyProfilePicture,
                    spotifyUsername = spotifyUsername,
                    selectedPage = selectedPage,
                    onPageSelected = { page ->
                        selectedPage = page
                    },
                    currentTrackName = currentTrackName,
                    currentArtistName = currentArtistName,
                    currentPlaylistName = currentPlaylistName,
                    playlistStatus = playlistStatus,
                    albumArtwork = albumArtwork,
                    isPlaying = isPlaying,
                    hasCurrentTrack = hasCurrentTrack,
                    playlists = playlists,
                    currentPositionMs = currentPositionMs,
                    trackDurationMs = trackDurationMs,
                    playbackPositionUpdatedAt = playbackPositionUpdatedAt,
                    onSeek = { position ->
                        spotifyAppRemote?.playerApi
                            ?.seekTo(position)
                            ?.setErrorCallback { error ->
                                Log.e("Cloudberry", "Failed to seek", error)
                            }
                    },
                    onConnectSpotify = { connectToSpotify() },
                    onAuthorizeSpotify = { authorizePlaylistAccess() },

                    onPrevious = {
                        pendingTrackDirection = -1
                        spotifyAppRemote?.playerApi?.skipPrevious()
                    },
                    onPlayPause = {
                        val playerApi = spotifyAppRemote?.playerApi

                        if (playerApi != null) {
                            if (isPlaying) {
                                playerApi.pause()
                            } else {
                                playerApi.resume()
                            }
                        }
                    },
                    onNext = {
                        pendingTrackDirection = 1
                        spotifyAppRemote?.playerApi?.skipNext()
                    },
                    onQueuePlaylist = { playlist ->
                        pendingPlaylist = playlist

                        Log.d(
                            "Cloudberry",
                            "Queued playlist for next track: ${playlist.name}"
                        )
                    },
                    onPlayPlaylist = { playlist ->
                        currentPlaylistName = playlist.name
                        startPlaylistCounter(playlist)

                        spotifyAppRemote?.playerApi
                            ?.play("spotify:playlist:${playlist.id}")
                            ?.setErrorCallback { error ->
                                currentPlaylistName = ""
                                Log.e(
                                    "Cloudberry",
                                    "Failed to play playlist: ${playlist.name}",
                                    error
                                )
                            }
                    },
                    onRequestPlaylistAccess = {
                        if (isAuthorizing) {
                            // Authorization is already in progress.
                        } else if (spotifyAccessToken != null) {
                            Thread {
                                loadPlaylistsWithAccessToken(spotifyAccessToken!!)
                            }.start()
                        } else {
                            authorizePlaylistAccess()
                        }
                    },
                )
            }
        }
        restoreSpotifyAuthorization()
    }



    private fun loadSpotifyProfilePicture(accessToken: String) {
        Thread {
            try {
                val connection = URL(
                    "https://api.spotify.com/v1/me"
                ).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken"
                )
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val status = connection.responseCode

                if (status !in 200..299) {
                    Log.e(
                        "Cloudberry",
                        "Profile request failed: HTTP $status"
                    )
                    connection.disconnect()
                    return@Thread
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val profileJson = JSONObject(response)

                // Read the Spotify display name.
                val username = profileJson.optString(
                    "display_name",
                    ""
                )

                // Update the username even if no profile picture exists.
                runOnUiThread {
                    spotifyUsername = username
                }

                // Read the profile picture URL.
                val images = profileJson.optJSONArray("images")
                val imageUrl = images
                    ?.optJSONObject(0)
                    ?.optString("url")
                    ?.takeIf { it.isNotBlank() }

                if (imageUrl == null) {
                    runOnUiThread {
                        spotifyProfilePicture = null
                    }
                    Log.d("Cloudberry", "No Spotify profile picture found")
                    return@Thread
                }

                val imageConnection = URL(imageUrl)
                    .openConnection() as HttpURLConnection

                imageConnection.connectTimeout = 10000
                imageConnection.readTimeout = 10000

                val bitmap = imageConnection.inputStream.use {
                    BitmapFactory.decodeStream(it)
                }

                imageConnection.disconnect()

                runOnUiThread {
                    spotifyProfilePicture = bitmap
                }
            } catch (error: Exception) {
                Log.e(
                    "Cloudberry",
                    "Could not load Spotify profile",
                    error
                )
            }
        }.start()
    }



    private fun connectToSpotify() {
        if (!isSpotifyLoggedIn) return
        if (spotifyAppRemote != null || isConnecting) return

        isConnecting = true
        connectionStatus = "Connecting to Spotify..."

        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(appRemoteRedirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(
            this,
            params,
            object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    runOnUiThread {
                        spotifyAppRemote = appRemote
                        isConnecting = false
                        connectionStatus = "Connected to Spotify"
                        observeSpotifyPlayer(appRemote)
                    }
                    Log.d("Cloudberry", "App Remote connected")
                }

                override fun onFailure(error: Throwable) {
                    Log.e("Cloudberry", "App Remote connection failed", error)
                    runOnUiThread {
                        isConnecting = false
                        connectionStatus =
                            "Connection failed: ${error.localizedMessage ?: "Unknown error"}"
                    }
                }
            }
        )
    }


    private fun logoutSpotify() {
        // Disconnect playback controls.
        spotifyAppRemote?.let { remote ->
            SpotifyAppRemote.disconnect(remote)
        }
        spotifyAppRemote = null
        lastCheckedTrackUri = null
        // Clear saved and in-memory playlist authorization.
        SpotifyTokenStorage(this).clearTokens()
        spotifyAccessToken = null
        authorizationCode = null
        playlists = emptyList()
        spotifyProfilePicture = null
        spotifyUsername = ""
        isSpotifyLoggedIn = false
        // Reset Spotify-related UI state.
        connectionStatus = "not connected"
        isConnecting = false
        authStatus = "playlist access not authorized"
        isAuthorizing = false
        currentPlaylistName = ""
        playlistStatus = "connect to spotify first"
        currentTrackName = ""
        currentArtistName = ""
        albumArtwork = null
        isPlaying = false
        hasCurrentTrack = false
        currentPositionMs = 0L
        trackDurationMs = 0L
        playbackPositionUpdatedAt = 0L
    }

    private fun handleSpotifyLoginLogout() {
        val tokenStorage = SpotifyTokenStorage(this)
        val savedRefreshToken = tokenStorage.getRefreshToken()

        if (savedRefreshToken != null || spotifyAccessToken != null) {
            logoutSpotify()
        } else {
            authorizePlaylistAccess()
        }
    }


    private fun authorizePlaylistAccess() {
        if (isAuthorizing) return

        isAuthorizing = true
        authStatus = "Opening Spotify authorization..."

        val request = AuthorizationRequest.Builder(
            clientId,
            AuthorizationResponse.Type.CODE,
            authRedirectUri
        )
            .setScopes(
                arrayOf(
                    "playlist-read-private",
                    "playlist-read-collaborative",
                    "user-read-playback-state"
                )
            )
            .build()

        try {
            AuthorizationClient.openLoginActivity(
                this,
                authRequestCode,
                request
            )
        } catch (error: Exception) {
            isAuthorizing = false
            authStatus =
                "could not start authorization: ${error.localizedMessage ?: "Unknown error"}"
            Log.e("Cloudberry", "Could not start authorization", error)
        }
    }

    @Deprecated("Uses Spotify Auth Library's documented activity-result flow")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != authRequestCode) return

        isAuthorizing = false

        val response = AuthorizationClient.getResponse(resultCode, data)

        when (response.type) {
            AuthorizationResponse.Type.CODE -> {
                val code = response.code

                if (!code.isNullOrBlank()) {
                    Log.d("Cloudberry", "Spotify authorization code received")
                    exchangeCodeAndLoadPlaylists(code)
                } else {
                    authStatus = "Authorization failed: No authorization code received."
                    Log.e("Cloudberry", authStatus)
                }
            }

            AuthorizationResponse.Type.ERROR -> {
                authStatus =
                    "Authorization failed: ${response.error ?: "Unknown error"}"
                Log.e("Cloudberry", authStatus)
            }

            else -> {
                authStatus = "Authorization cancelled or not completed."
                Log.d("Cloudberry", "Authorization did not complete")
            }
        }
    }


    private fun exchangeCodeAndLoadPlaylists(code: String) {
        authStatus = "Exchanging code and loading playlists..."

        Thread {
            try {
                // Exchange the authorization code through FastAPI.
                val tokenConnection = URL(
                    "$backendBaseUrl/api/spotify/token"
                ).openConnection() as HttpURLConnection

                tokenConnection.requestMethod = "POST"
                tokenConnection.connectTimeout = 15000
                tokenConnection.readTimeout = 15000
                tokenConnection.doOutput = true
                tokenConnection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val body = JSONObject()
                    .put("code", code)
                    .toString()

                tokenConnection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val tokenStatus = tokenConnection.responseCode
                val tokenBody = (
                        if (tokenStatus in 200..299) {
                            tokenConnection.inputStream
                        } else {
                            tokenConnection.errorStream
                        }
                        )?.bufferedReader()?.use { it.readText() }.orEmpty()

                tokenConnection.disconnect()

                if (tokenStatus !in 200..299) {
                    throw IllegalStateException(
                        "Token exchange failed ($tokenStatus): $tokenBody"
                    )
                }

                val tokenJson = JSONObject(tokenBody)

                val accessToken = tokenJson.getString("access_token")
                runOnUiThread {
                    spotifyAccessToken = accessToken
                    isSpotifyLoggedIn = true
                    loadSpotifyProfilePicture(accessToken)
                    authStatus = "Playlist access authorized"
                    runOnUiThread {
                        if (spotifyAppRemote == null && !isConnecting) {
                            connectToSpotify()
                        }
                    }
                }

                val refreshToken = tokenJson.optString("refresh_token")
                    .takeIf { it.isNotBlank() }

                if (refreshToken != null) {
                    SpotifyTokenStorage(this@MainActivity)
                        .saveRefreshToken(refreshToken)
                }

                // Fetch the user's Spotify playlists using the reusable function.
                loadPlaylistsWithAccessToken(accessToken)

            } catch (error: Exception) {
                Log.e(
                    "Cloudberry",
                    "Token exchange or playlist loading failed",
                    error
                )

                runOnUiThread {
                    authStatus =
                        "could not load playlists: ${error.message ?: "Unknown error"}"
                }
            }
        }.start()
    }


    private fun restoreSpotifyAuthorization() {
        val refreshToken = SpotifyTokenStorage(this).getRefreshToken()

        if (refreshToken.isNullOrBlank()) {
            authStatus = "Please authorize Spotify playlist access."
            return
        }

        authStatus = "Restoring Spotify playlist access..."

        Thread {
            try {
                val connection = URL(
                    "$backendBaseUrl/api/spotify/refresh"
                ).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val body = JSONObject()
                    .put("refresh_token", refreshToken)
                    .toString()

                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode
                val responseBody = (
                        if (status in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }
                        )?.bufferedReader()?.use { it.readText() }.orEmpty()

                connection.disconnect()

                if (status !in 200..299) {
                    throw IllegalStateException(
                        "Token refresh failed ($status): $responseBody"
                    )
                }

                val tokenJson = JSONObject(responseBody)
                val accessToken = tokenJson.getString("access_token")
                runOnUiThread {
                    spotifyAccessToken = accessToken
                    isSpotifyLoggedIn = true
                    loadSpotifyProfilePicture(accessToken)
                    authStatus = "Playlist access authorized"
                    runOnUiThread {
                        if (spotifyAppRemote == null && !isConnecting) {
                            connectToSpotify()
                        }
                    }
                }

                // Spotify may return a replacement refresh token.
                val newRefreshToken = tokenJson.optString("refresh_token")
                if (newRefreshToken.isNotBlank()) {
                    SpotifyTokenStorage(this).saveRefreshToken(newRefreshToken)
                }

                loadPlaylistsWithAccessToken(accessToken)

            } catch (error: Exception) {
                Log.e("Cloudberry", "Could not restore Spotify authorization", error)

                runOnUiThread {
                    authStatus =
                        "Could not restore Spotify access: ${error.message ?: "Unknown error"}"
                }
            }
        }.start()
    }



    private fun loadPlaylistsWithAccessToken(accessToken: String) {
        Thread {
            try {
                val loadedPlaylists = mutableListOf<SpotifyPlaylist>()
                var offset = 0
                val limit = 50
                var hasMore = true

                while (hasMore) {
                    val url =
                        "https://api.spotify.com/v1/me/playlists?limit=$limit&offset=$offset"

                    val connection =
                        URL(url).openConnection() as HttpURLConnection

                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $accessToken"
                        )

                        val status = connection.responseCode
                        val responseBody = (
                                if (status in 200..299) {
                                    connection.inputStream
                                } else {
                                    connection.errorStream
                                }
                                )?.bufferedReader()?.use { it.readText() }.orEmpty()

                        if (status !in 200..299) {
                            throw IllegalStateException(
                                "Playlist request failed ($status): $responseBody"
                            )
                        }

                        val response = JSONObject(responseBody)
                        val items = response.getJSONArray("items")

                        for (index in 0 until items.length()) {
                            val item = items.getJSONObject(index)
                            val images = item.optJSONArray("images")

                            val imageUrl = images
                                ?.optJSONObject(0)
                                ?.optString("url")
                                ?.takeIf { it.isNotBlank() }

                            loadedPlaylists.add(
                                SpotifyPlaylist(
                                    id = item.getString("id"),
                                    name = item.optString(
                                        "name",
                                        "Unnamed playlist"
                                    ),
                                    imageUrl = imageUrl
                                )
                            )
                        }

                        // Use Spotify's pagination metadata to determine
                        // whether another page exists.
                        hasMore = !response.isNull("next") &&
                                response.optString("next").isNotBlank()

                        offset += items.length()

                        // Prevent an endless loop if Spotify returns
                        // an empty page while indicating more results.
                        if (items.length() == 0) {
                            hasMore = false
                        }
                    } finally {
                        connection.disconnect()
                    }
                }

                runOnUiThread {
                    playlists = loadedPlaylists
                    authStatus =
                        "Playlist access restored: ${loadedPlaylists.size} playlists loaded."
                }
            } catch (error: Exception) {
                Log.e(
                    "Cloudberry",
                    "could not load spotify playlists",
                    error
                )

                runOnUiThread {
                    authStatus =
                        "could not load playlists: ${error.message ?: "Unknown error"}"
                }
            }
        }.start()
    }




    private fun updateCurrentPlaylist() {
        val accessToken = spotifyAccessToken

        if (accessToken == null) {
            currentPlaylistName = ""
            playlistStatus = "connect to spotify"
            return
        }

        // Prevent a new request while the previous one is still running.
        if (isUpdatingPlaylist) return
        isUpdatingPlaylist = true

        Thread {
            var connection: HttpURLConnection? = null

            try {
                connection = URL(
                    "https://api.spotify.com/v1/me/player"
                ).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken"
                )

                val status = connection.responseCode

                if (status == 204) {
                    runOnUiThread {
                        currentPlaylistName = ""
                        playlistStatus = "nothing playing right now"
                    }
                    return@Thread
                }

                val responseBody = (
                        if (status in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }
                        )?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (status !in 200..299) {
                    Log.e(
                        "Cloudberry",
                        "Playback API error $status: $responseBody"
                    )

                    runOnUiThread {
                        if (status == 401) {
                            playlistStatus = "spotify authorization expired"
                        } else {
                            playlistStatus = "could not load current playlist"
                        }
                    }
                    return@Thread
                }

                val json = JSONObject(responseBody)
                val context = json.optJSONObject("context")
                val contextUri = context?.optString("uri").orEmpty()
                val contextType = context?.optString("type").orEmpty()

                if (
                    contextType != "playlist" ||
                    !contextUri.startsWith("spotify:playlist:")
                ) {
                    runOnUiThread {
                        currentPlaylistName = ""
                        playlistStatus = "playing outside a playlist"
                    }
                    return@Thread
                }

                val playlistId = contextUri.substringAfterLast(":")

                // First search the playlists already loaded into Cloudberry.
                val savedPlaylist = playlists.firstOrNull {
                    it.id == playlistId
                }

                if (savedPlaylist != null) {
                    runOnUiThread {
                        currentPlaylistName = savedPlaylist.name
                        playlistStatus = ""
                    }
                    return@Thread
                }

                // The playlist isn't in the user's loaded playlist list.
                var playlistConnection: HttpURLConnection? = null

                try {
                    playlistConnection = URL(
                        "https://api.spotify.com/v1/playlists/$playlistId"
                    ).openConnection() as HttpURLConnection

                    playlistConnection.requestMethod = "GET"
                    playlistConnection.connectTimeout = 10000
                    playlistConnection.readTimeout = 10000
                    playlistConnection.setRequestProperty(
                        "Authorization",
                        "Bearer $accessToken"
                    )

                    val playlistStatusCode = playlistConnection.responseCode

                    val playlistBody = (
                            if (playlistStatusCode in 200..299) {
                                playlistConnection.inputStream
                            } else {
                                playlistConnection.errorStream
                            }
                            )?.bufferedReader()?.use { it.readText() }.orEmpty()

                    if (playlistStatusCode !in 200..299) {
                        Log.e(
                            "Cloudberry",
                            "Playlist details error $playlistStatusCode: $playlistBody"
                        )

                        runOnUiThread {
                            currentPlaylistName = ""
                            playlistStatus = "could not load playlist details"
                        }
                        return@Thread
                    }

                    val playlistJson = JSONObject(playlistBody)
                    val playlistName = playlistJson.optString("name")

                    runOnUiThread {
                        if (playlistName.isNotBlank()) {
                            currentPlaylistName = playlistName
                            playlistStatus = ""
                        } else {
                            currentPlaylistName = ""
                            playlistStatus = "Playlist name unavailable"
                        }
                    }
                } finally {
                    playlistConnection?.disconnect()
                }

            } catch (error: Exception) {
                Log.e(
                    "Cloudberry",
                    "Could not detect the current playlist",
                    error
                )

                runOnUiThread {
                    playlistStatus = "could not load current playlist"
                }
            } finally {
                connection?.disconnect()
                isUpdatingPlaylist = false
            }
        }.start()
    }



    private fun startPlaylistCounter(playlist: SpotifyPlaylist) {
        counterPlaylistId = playlist.id
        counterPlaylistName = playlist.name
        currentTrackCounter = 0
        totalPlaylistTracks = 0
        counterLastTrackUri = null
        counterLastTrackIndex = -1
        playlistTrackUris = emptyList()
        ignoreNextTrackChangeForCounter = true

        Log.d(
            "CloudberryCounter",
            "Loading ordered track URIs for ${playlist.name}"
        )

        val accessToken = spotifyAccessToken ?: run {
            Log.e("CloudberryCounter", "Cannot load tracks: no Spotify access token")
            return
        }

        Thread {
            var offset = 0
            val limit = 50
            val loadedUris = mutableListOf<String>()
            var total = 0

            try {
                while (true) {
                    val url = URL(
                        "https://api.spotify.com/v1/playlists/${playlist.id}/items" +
                                "?limit=$limit&offset=$offset"
                    )

                    val connection = url.openConnection() as HttpURLConnection

                    try {
                        connection.requestMethod = "GET"
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $accessToken"
                        )
                        connection.setRequestProperty(
                            "Accept",
                            "application/json"
                        )
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000

                        val status = connection.responseCode
                        val responseBody = (
                                if (status in 200..299) {
                                    connection.inputStream
                                } else {
                                    connection.errorStream
                                }
                                )?.bufferedReader()?.use { it.readText() }.orEmpty()

                        if (status !in 200..299) {
                            throw IllegalStateException(
                                "HTTP $status: $responseBody"
                            )
                        }

                        val json = JSONObject(responseBody)
                        total = json.optInt("total", total)
                        val items = json.optJSONArray("items")
                            ?: throw IllegalStateException(
                                "Spotify response did not contain an items array"
                            )

                        for (i in 0 until items.length()) {
                            val entry = items.optJSONObject(i) ?: continue

                            // Spotify responses may expose the playable item as
                            // "item" or, in older response formats, "track".
                            val track = entry.optJSONObject("item")
                                ?: entry.optJSONObject("track")
                                ?: continue

                            val uri = track.optString("uri")
                            if (
                                uri.startsWith("spotify:track:") ||
                                uri.startsWith("spotify:episode:")
                            ) {
                                loadedUris.add(uri)
                            }
                        }

                        if (items.length() == 0 || offset + items.length() >= total) {
                            break
                        }

                        offset += items.length()
                    } finally {
                        connection.disconnect()
                    }
                }

                runOnUiThread {
                    if (counterPlaylistId == playlist.id) {
                        playlistTrackUris = loadedUris
                        totalPlaylistTracks = total

                        Log.d(
                            "CloudberryCounter",
                            "Loaded ${playlistTrackUris.size} track URIs " +
                                    "for ${playlist.name}; Spotify total: $totalPlaylistTracks"
                        )
                    }
                }
            } catch (error: Exception) {
                Log.e(
                    "CloudberryCounter",
                    "Could not load ordered tracks for ${playlist.name}",
                    error
                )
            }
        }.start()
    }



    private fun observeSpotifyPlayer(appRemote: SpotifyAppRemote) {
        appRemote.playerApi.subscribeToPlayerState()
            .setEventCallback { playerState ->
                val track = playerState.track
                val trackUri = track?.uri

                if (trackUri != null && trackUri != lastCheckedTrackUri) {
                    lastCheckedTrackUri = trackUri

                    // Count the new track if it belongs to the playlist being counted.


                    // Update the counter when the track changes.
                    if (counterPlaylistId != null && trackUri != null) {
                        val trackIndex = playlistTrackUris.indexOf(trackUri)

                        if (trackIndex >= 0) {
                            currentTrackCounter = trackIndex + 1
                            counterLastTrackIndex = trackIndex
                            counterLastTrackUri = trackUri
                            ignoreNextTrackChangeForCounter = false

                            Log.d(
                                "CloudberryCounter",
                                "$counterPlaylistName: $currentTrackCounter/$totalPlaylistTracks"
                            )
                        } else {
                            Log.d(
                                "CloudberryCounter",
                                "Current track not found in $counterPlaylistName: $trackUri"
                            )
                        }
                    }



                    val playlistToApply = pendingPlaylist

                    if (playlistToApply != null) {
                        // Consume the queued playlist so it isn't applied repeatedly.
                        pendingPlaylist = null

                        Log.d(
                            "Cloudberry",
                            "Track changed. Preparing playlist switch: ${playlistToApply.name}"
                        )

                        runOnUiThread {
                            isSwitchingPlaylist = true
                            playlistStatus = "loading..."
                        }

                        Thread {
                            try {
                                // Give Spotify a moment to settle after the track changes.
                                Thread.sleep(300)

                                runOnUiThread {
                                    val playerApi = spotifyAppRemote?.playerApi
                                    val playlistUri = "spotify:playlist:${playlistToApply.id}"

                                    if (playerApi == null) {
                                        Log.e("Cloudberry", "Spotify is not connected")
                                        isSwitchingPlaylist = false
                                        return@runOnUiThread
                                    }

                                    playerApi.play(playlistUri)
                                        .setResultCallback {
                                            playerApi.skipToIndex(playlistUri, 0)
                                                .setResultCallback {
                                                    Log.d(
                                                        "Cloudberry",
                                                        "Restarted queued playlist from index 0: ${playlistToApply.name}"
                                                    )
                                                    startPlaylistCounter(playlistToApply)

                                                    Thread {
                                                        try {
                                                            Thread.sleep(300)
                                                        } finally {
                                                            runOnUiThread {
                                                                isSwitchingPlaylist = false
                                                                updateCurrentPlaylist()
                                                            }
                                                        }
                                                    }.start()
                                                }
                                                .setErrorCallback { error ->
                                                    Log.e(
                                                        "Cloudberry",
                                                        "Failed to restart queued playlist: ${playlistToApply.name}",
                                                        error
                                                    )

                                                    runOnUiThread {
                                                        isSwitchingPlaylist = false
                                                        updateCurrentPlaylist()
                                                    }
                                                }
                                        }
                                        .setErrorCallback { error ->
                                            Log.e(
                                                "Cloudberry",
                                                "Failed to start queued playlist: ${playlistToApply.name}",
                                                error
                                            )

                                            runOnUiThread {
                                                isSwitchingPlaylist = false
                                                updateCurrentPlaylist()
                                            }
                                        }
                                }
                            } catch (error: InterruptedException) {
                                Thread.currentThread().interrupt()

                                runOnUiThread {
                                    isSwitchingPlaylist = false
                                    updateCurrentPlaylist()
                                }
                            }
                        }.start()
                    } else {
                        // No queued playlist: check the playlist for the new track.
                        updateCurrentPlaylist()
                    }
                }


                runOnUiThread {
                    if (track != null) {
                        currentTrackName = track.name
                        currentArtistName = track.artist.name
                        isPlaying = !playerState.isPaused
                        hasCurrentTrack = true
                        currentPositionMs = playerState.playbackPosition
                        trackDurationMs = track.duration
                        playbackPositionUpdatedAt = android.os.SystemClock.elapsedRealtime()

                        appRemote.imagesApi
                            .getImage(track.imageUri)
                            .setResultCallback { bitmap ->
                                runOnUiThread {
                                    albumArtwork = bitmap
                                }
                            }
                            .setErrorCallback { error ->
                                Log.e(
                                    "Cloudberry",
                                    "Could not load album artwork",
                                    error
                                )
                            }
                    } else {
                        currentTrackName = ""
                        currentArtistName = ""
                        albumArtwork = null
                        isPlaying = false
                        hasCurrentTrack = false
                        currentPositionMs = 0L
                        trackDurationMs = 0L
                        playbackPositionUpdatedAt = 0L
                    }
                }
            }
            .setErrorCallback { error ->
                Log.e(
                    "Cloudberry",
                    "Could not subscribe to Spotify player state",
                    error
                )
            }
    }


    override fun onDestroy() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        spotifyAppRemote = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudberryHome(
    connectionStatus: String,
    backendBaseUrl : String,
    spotifyAccessToken: String?,
    authStatus: String,
    isConnecting: Boolean,
    isAuthorizing: Boolean,
    isSpotifyLoggedIn: Boolean,
    isSwitchingPlaylist: Boolean,
    onLogoutSpotify: () -> Unit,
    onLoginLogout: () -> Unit,
    spotifyProfilePicture: Bitmap?,
    spotifyUsername: String,
    selectedPage: String,
    onPageSelected: (String) -> Unit,
    currentTrackName: String,
    currentArtistName: String,
    currentPlaylistName: String,
    playlistStatus: String,
    albumArtwork: android.graphics.Bitmap?,
    isPlaying: Boolean,
    hasCurrentTrack: Boolean,
    playlists: List<SpotifyPlaylist>,
    currentPositionMs: Long,
    trackDurationMs: Long,
    playbackPositionUpdatedAt: Long,
    onSeek: (Long) -> Unit,
    onConnectSpotify: () -> Unit,
    onAuthorizeSpotify: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onQueuePlaylist: (SpotifyPlaylist) -> Unit,
    onPlayPlaylist: (SpotifyPlaylist) -> Unit,
    onRequestPlaylistAccess: () -> Unit,
) {
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var userLatitude by remember {
        mutableStateOf<Double?>(null)
    }

    var userLongitude by remember {
        mutableStateOf<Double?>(null)
    }

    var locationStatus by remember {
        mutableStateOf("Location not requested")
    }

    var weatherCity by remember {
        mutableStateOf("")
    }

    var weatherCountry by remember {
        mutableStateOf("")
    }

    var weatherTemperature by remember {
        mutableStateOf<Double?>(null)
    }

    var weatherCondition by remember {
        mutableStateOf("")
    }

    var weatherStatus by remember {
        mutableStateOf("Waiting for location")
    }
    var isRaining by remember {
        mutableStateOf(false)
    }

    var detectedSeason by remember {
        mutableStateOf(detectSeason(Calendar.getInstance()))
    }

    var currentTime by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()

            detectedSeason = detectSeason(now)

            currentTime = String.format(
                "%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)
            )

            delay(60_000)
        }
    }

    fun fetchWeather(latitude: Double, longitude: Double) {
        weatherStatus = "Loading weather..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val url = URL(
                    "$backendBaseUrl/api/weather" +
                            "?latitude=$latitude&longitude=$longitude"
                )

                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val status = connection.responseCode

                val responseBody = (
                        if (status in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }
                        )?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (status !in 200..299) {
                    throw IllegalStateException(
                        "Weather request failed ($status): $responseBody"
                    )
                }

                val json = JSONObject(responseBody)

                val city = json.optString("city", "")
                val country = json.optString("country", "")
                val temperature = json.getDouble("temperature_c")
                val condition = json.optString("condition", "")
                val rainDetected = json.getBoolean("is_raining")
                val precipitation = json.getDouble("precip_mm")

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    weatherCity = city
                    weatherCountry = country
                    weatherTemperature = temperature
                    weatherCondition = condition
                    weatherStatus = "Weather loaded"
                    isRaining = rainDetected

// Rain detection
                    Log.d(
                        "Cloudberry",
                        "Weather loaded: $city, $condition, " +
                                "${temperature}°C, raining=$isRaining, " +
                                "precipitation=${precipitation}mm"
                    )
                }
            } catch (error: Exception) {
                Log.e("Cloudberry", "Failed to fetch weather", error)

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    weatherStatus = "Could not load weather"
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                locationStatus = "Location permission granted"

                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    ).addOnSuccessListener { location ->

                        if (location != null) {
                            userLatitude = location.latitude
                            userLongitude = location.longitude
                            locationStatus = "Location obtained"
                            fetchWeather(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )

                            Log.d(
                                "Cloudberry",
                                "Location obtained: ${location.latitude}, ${location.longitude}"
                            )
                        } else {
                            locationStatus = "Location unavailable"
                            Log.w("Cloudberry", "Location result was null")
                        }

                    }.addOnFailureListener { error ->
                        locationStatus = "Could not obtain location"
                        Log.e("Cloudberry", "Location request failed", error)
                    }
                } catch (error: SecurityException) {
                    locationStatus = "Location permission unavailable"
                    Log.e("Cloudberry", "Location permission error", error)
                }
            } else {
                locationStatus = "Location permission denied"
                Log.d("Cloudberry", locationStatus)
            }
        }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        userLatitude = location.latitude
                        userLongitude = location.longitude
                        locationStatus = "Location obtained"
                        fetchWeather(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )

                        Log.d(
                            "Cloudberry",
                            "Location obtained: ${location.latitude}, ${location.longitude}"
                        )
                    } else {
                        locationStatus = "Location unavailable"
                    }
                }.addOnFailureListener { error ->
                    locationStatus = "Could not obtain location"
                    Log.e("Cloudberry", "Location request failed", error)
                }
            } catch (error: SecurityException) {
                locationStatus = "Location permission unavailable"
                Log.e("Cloudberry", "Location permission error", error)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val ruleStorage = remember {
        RuleStorage()
    }

    var savedRules by remember {
        mutableStateOf<List<PlaylistRule>>(emptyList())
    }

    var rulesLoaded by remember {
        mutableStateOf(false)
    }

    var ruleBeingEdited by remember {
        mutableStateOf<PlaylistRule?>(null)
    }

    var rulePriority by remember {
        mutableStateOf("time")
    }




    LaunchedEffect(spotifyAccessToken, ruleStorage) {
        rulesLoaded = false

        val token = spotifyAccessToken

        if (token.isNullOrBlank()) {
            savedRules = emptyList()
            return@LaunchedEffect
        }

        try {
            val loadedRules = ruleStorage.loadRulesFromBackend(
                accessToken = token,
                backendBaseUrl = backendBaseUrl
            )

            savedRules = loadedRules
            rulesLoaded = true

            Log.d("Cloudberry", "Loaded ${loadedRules.size} rules from backend")

            // TEST: log the playlist order and track counts for every saved rule.
            loadedRules.forEach { rule ->
                logRulePlaylistOrderAndTrackCounts(
                    rule = rule,
                    accessToken = token
                )
            }

        } catch (error: Exception) {
            Log.e("Cloudberry", "Failed to load rules from backend", error)
        }
    }




    var lastTriggeredRuleId by remember {
        mutableStateOf<String?>(null)
    }
    var currentRuleName by remember {
        mutableStateOf("")
    }

    var pendingPlaylist by remember {
        mutableStateOf<SpotifyPlaylist?>(null)
    }


    LaunchedEffect(
        selectedPage,
        rulesLoaded,
        savedRules,
        playlists,
        isRaining,
        rulePriority
    ) {
        // Only run the scheduler while Home is visible.
        if (selectedPage != "home") {
            return@LaunchedEffect
        }

        if (!rulesLoaded || savedRules.isEmpty()) {
            lastTriggeredRuleId = null
            return@LaunchedEffect
        }

        // Check immediately, then repeat while Home stays open.
        while (selectedPage == "home") {

            val now = Calendar.getInstance()
            val detectedSeason = detectSeason(now)

            val matchingRules = savedRules.filter { rule ->
                matchesPlaylistRule(
                    rule = rule,
                    detectedSeason = detectedSeason,
                    currentTime = now,
                    isRaining = isRaining
                )
            }

            val matchingRule = matchingRules.firstOrNull {
                it.triggerOption.equals("season and time", ignoreCase = true)
            } ?: matchingRules.firstOrNull {
                if (rulePriority.equals("time", ignoreCase = true)) {
                    it.triggerOption.equals("time only", ignoreCase = true)
                } else {
                    it.triggerOption.equals("season only", ignoreCase = true)
                }
            } ?: matchingRules.firstOrNull()
            if (matchingRule == null) {
                lastTriggeredRuleId = null
            } else if (lastTriggeredRuleId != matchingRule.id) {
                val orderedPlaylists = matchingRule.playlistOrder
                    .filter { selected ->
                        matchingRule.playlists.any {
                            it.id == selected.id
                        }
                    }

                val availablePlaylists = if (orderedPlaylists.isNotEmpty()) {
                    orderedPlaylists
                } else {
                    matchingRule.playlists
                }

                val selectedPlaylist = if (matchingRule.randomOrder) {
                    availablePlaylists.randomOrNull()
                } else {
                    availablePlaylists.firstOrNull()
                }

                if (selectedPlaylist != null) {
                    val spotifyPlaylist = playlists.firstOrNull {
                        it.id == selectedPlaylist.id
                    }

                    if (spotifyPlaylist != null) {
                        val currentTimeText = String.format(
                            "%02d:%02d",
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE)
                        )

                        Log.d(
                            "Cloudberry",
                            "Rule '${matchingRule.name}' matched. " +
                                    "Season: $detectedSeason, " +
                                    "time: $currentTimeText, " +
                                    "playlist: ${spotifyPlaylist.name}"
                        )

                        if (hasCurrentTrack) {
                            // Keep the current song playing.
                            // Apply the latest matching playlist when the track changes.
                            onQueuePlaylist(spotifyPlaylist)

                            Log.d(
                                "Cloudberry",
                                "Playlist change queued until track changes: ${spotifyPlaylist.name}"
                            )
                        } else {
                            // Nothing is playing, so start the selected playlist immediately.
                            onPlayPlaylist(spotifyPlaylist)
                        }

                        lastTriggeredRuleId = matchingRule.id
                        currentRuleName = matchingRule.name
                    } else {
                        Log.w(
                            "Cloudberry",
                            "Matching playlist isn't currently loaded: " +
                                    selectedPlaylist.name
                        )
                    }
                }
            }

            delay(5_000)
        }
    }

    BackHandler(enabled = selectedPage != "home") {
        when (selectedPage) {
            "add playlist rule" -> {
                onPageSelected("rules")
            }

            "rules" -> {
                onPageSelected("home")
            }

            "settings & info" -> {
                onPageSelected("home")
            }
        }
    }

    val transition = rememberInfiniteTransition(
        label = "vinyl rotation"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl angle"
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFEFC),
            Color(0xFFFFF5E8),
            Color(0xFFFFD6A0)
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedPage != "add playlist rule",

        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFFFBF5),
                modifier = Modifier.width(280.dp)
            ) {
                Text(
                    text = "cloudberry",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF49382D),
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 32.dp,
                        bottom = 8.dp
                    )
                )

                Text(
                    text = "your music, your time",
                    fontSize = 13.sp,
                    color = Color(0xFF8C7868),
                    modifier = Modifier.padding(
                        start = 24.dp,
                        bottom = 28.dp
                    )
                )

                HorizontalDivider(color = Color(0xFFEADFD2))

                val pages = listOf(
                    "home",
                    "rules",
                    "settings & info"
                )

                pages.forEach { page ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = page,
                                fontSize = 15.sp
                            )
                        },
                        selected = selectedPage == page,
                        onClick = {
                            onPageSelected(page)

                            scope.launch {
                                drawerState.close()
                            }
                        },
                        shape = RectangleShape,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFF4E5D4),
                            selectedTextColor = Color(0xFF49382D),
                            unselectedTextColor = Color(0xFF8C7868)
                        )
                    )
                }
            }
        }

    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Full-screen gradient background for the home page only.
            if (selectedPage == "home") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                )
            }

            // Main content stays above the background.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 26.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedPage != "add playlist rule") {
                    // top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                            .offset(x = (-4).dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .padding(0.dp)
                        ) {

                            if (spotifyProfilePicture != null) {
                                Image(
                                    bitmap = spotifyProfilePicture.asImageBitmap(),
                                    contentDescription = "Spotify profile picture",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "open navigation menu",
                                    tint = Color(0xFF795238),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }


                        Text(
                            text = "cloudberry",
                            fontSize = 27.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF49382D),
                            letterSpacing = (-0.8).sp
                        )
                    }
                }

                if (selectedPage == "home") {

                    Spacer(modifier = Modifier.weight(0.7f))

                    Text(
                        text = when {
                            isSwitchingPlaylist ->
                                "loading..."

                            playlistStatus == "Loading playlist..." ->
                                "loading playlist..."

                            currentPlaylistName.isNotBlank() ->
                                "from: $currentPlaylistName"

                            else ->
                                "now playing"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF896B52),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (currentRuleName.isNotBlank()) {
                            "rule: $currentRuleName"
                        } else {
                            ""
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF896B52),
                        textAlign = TextAlign.Center
                    )

                    // vinyl record
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f)
                                .rotate(if (isPlaying && hasCurrentTrack) rotation else 0f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF494949),
                                            Color(0xFF242424),
                                            Color(0xFF111111)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // subtle vinyl grooves
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.58f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color(0xFFB9A18A)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (albumArtwork != null && hasCurrentTrack) {
                                    androidx.compose.foundation.Image(
                                        bitmap = albumArtwork.asImageBitmap(),
                                        contentDescription = "current album artwork",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFB9A18A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6E5544))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (hasCurrentTrack) {
                            currentTrackName
                        } else {
                            "nothing playing yet"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF49382D),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Text(
                        text = if (hasCurrentTrack) {
                            currentArtistName
                        } else {
                            "start listening on spotify"
                        },
                        fontSize = 15.sp,
                        color = Color(0xFF896B52),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(10.dp))

// song progress and seeking
                    var seekPosition by remember { mutableFloatStateOf(0f) }
                    var isSeeking by remember { mutableStateOf(false) }

                    val duration = trackDurationMs.coerceAtLeast(0L)
                    val livePositionMs by produceState(
                        initialValue = currentPositionMs,
                        currentPositionMs,
                        playbackPositionUpdatedAt,
                        isPlaying,
                        isSeeking,
                        trackDurationMs
                    ) {
                        while (true) {
                            val elapsedMs = if (isPlaying && playbackPositionUpdatedAt > 0L) {
                                android.os.SystemClock.elapsedRealtime() -
                                        playbackPositionUpdatedAt
                            } else {
                                0L
                            }

                            value = (
                                    currentPositionMs + elapsedMs
                                    ).coerceIn(0L, trackDurationMs.coerceAtLeast(0L))

                            delay(250)
                        }
                    }

                    val displayedPosition = if (isSeeking) {
                        seekPosition.toLong()
                    } else {
                        livePositionMs.coerceIn(0L, duration)
                    }

                    LaunchedEffect(currentPositionMs, isSeeking) {
                        if (!isSeeking) {
                            seekPosition = currentPositionMs.toFloat()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Slider(
                            value = if (duration > 0L) {
                                displayedPosition.toFloat().coerceIn(0f, duration.toFloat())
                            } else {
                                0f
                            },
                            onValueChange = { value ->
                                isSeeking = true
                                seekPosition = value
                            },
                            onValueChangeFinished = {
                                onSeek(seekPosition.toLong())
                                isSeeking = false
                            },
                            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                            enabled = hasCurrentTrack && duration > 0L,
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF49382D)
                                    ),
                                    thumbSize = androidx.compose.ui.unit.DpSize(
                                        width = 10.dp,
                                        height = 10.dp
                                    )
                                )
                            },


                            track = { sliderState ->
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 1.dp)
                                        .height(10.dp)
                                ) {
                                    val centerY = size.height / 5
                                    val thickness = 3.dp.toPx()
                                    val fraction = sliderState.value
                                        .coerceIn(sliderState.valueRange.start, sliderState.valueRange.endInclusive)
                                        .let {
                                            (it - sliderState.valueRange.start) /
                                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                                        }

                                    drawLine(
                                        color = Color(0x66B9A18A),
                                        start = Offset(0f, centerY),
                                        end = Offset(size.width, centerY),
                                        strokeWidth = thickness,
                                        cap = StrokeCap.Round
                                    )

                                    drawLine(
                                        color = Color(0xFF49382D),
                                        start = Offset(0f, centerY),
                                        end = Offset(size.width * fraction, centerY),
                                        strokeWidth = thickness,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1.dp)
                                .offset(y = (-20).dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatPlaybackTime(displayedPosition),
                                fontSize = 12.sp,
                                color = Color(0xFF896B52)
                            )

                            Text(
                                text = formatPlaybackTime(duration),
                                fontSize = 12.sp,
                                color = Color(0xFF896B52)
                            )
                        }
                    }

                    // playback controls
                    Row(
                        modifier = Modifier.offset(y = (-20).dp),
                        horizontalArrangement = Arrangement.spacedBy(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            enabled = hasCurrentTrack
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "previous track",
                                tint = Color(0xFF49382D),
                                modifier = Modifier.size(34.dp),
                            )
                        }

                        IconButton(
                            onClick = onPlayPause,
                            enabled = hasCurrentTrack,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF49382D))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (isPlaying) {
                                    "pause"
                                } else {
                                    "play"
                                },
                                tint = Color(0xFFFFF8EF),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            enabled = hasCurrentTrack
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "next track",
                                tint = Color(0xFF49382D),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.8f))
                } else if (selectedPage == "settings & info") {
                    SettingsScreen(
                        modifier = Modifier.weight(1f),
                        isLoggedIn = isSpotifyLoggedIn,
                        isConnecting = isConnecting,
                        isAuthorizing = isAuthorizing,
                        spotifyUsername = spotifyUsername,
                        spotifyProfilePicture = spotifyProfilePicture,
                        weatherCity = if (isSpotifyLoggedIn) weatherCity else "",
                        weatherCountry = if (isSpotifyLoggedIn) weatherCountry else "",
                        weatherTemperature = if (isSpotifyLoggedIn) weatherTemperature else null,
                        weatherCondition = if (isSpotifyLoggedIn) weatherCondition else "",
                        isRaining = if (isSpotifyLoggedIn) isRaining else false,
                        weatherStatus = if (isSpotifyLoggedIn) weatherStatus else "Weather unavailable",
                        season = if (isSpotifyLoggedIn) detectedSeason else "",
                        currentTime = if (isSpotifyLoggedIn) currentTime else "",
                        onLoginLogout = onLoginLogout
                    )


                }

                else if (selectedPage == "rules") {
                    TimeAndPlaylistsScreen(
                        modifier = Modifier.weight(1f),
                        rules = if (isSpotifyLoggedIn) savedRules else emptyList(),
                        priority = rulePriority,
                        onPriorityChange = { newPriority ->
                            rulePriority = newPriority
                        },
                        onAddClick = {
                            ruleBeingEdited = null
                            onPageSelected("add playlist rule")
                        },
                        onEditRule = { rule ->
                            ruleBeingEdited = rule
                            onPageSelected("add playlist rule")
                        },

                        onDeleteRule = { ruleToRemove ->
                            val updatedRules = savedRules.filterNot {
                                it.id == ruleToRemove.id
                            }

                            savedRules = updatedRules

                            val token = spotifyAccessToken

                            if (rulesLoaded && !token.isNullOrBlank()) {
                                scope.launch {
                                    try {
                                        ruleStorage.saveRulesToBackend(
                                            accessToken = token,
                                            backendBaseUrl = backendBaseUrl,
                                            rules = updatedRules
                                        )
                                        Log.d("Cloudberry", "Rules saved to backend after deletion")
                                    } catch (error: Exception) {
                                        Log.e("Cloudberry", "Failed to save deleted rule to backend", error)
                                    }
                                }
                            }
                        }
                    )
                }

                else if (selectedPage == "add playlist rule") {
                    AddPlaylistRuleScreen(
                        playlists = playlists,
                        existingRules = savedRules,
                        modifier = Modifier.weight(1f),
                        initialRule = ruleBeingEdited,
                        onBackClick = {
                            ruleBeingEdited = null
                            onPageSelected("rules")
                        },
                        onRequestPlaylistAccess = onRequestPlaylistAccess,

                        onSaveRule = { savedRule ->
                            val updatedRules = if (ruleBeingEdited != null) {
                                savedRules.map { existingRule ->
                                    if (existingRule.id == ruleBeingEdited!!.id) {
                                        savedRule.copy(id = existingRule.id)
                                    } else {
                                        existingRule
                                    }
                                }
                            } else {
                                savedRules + savedRule
                            }

                            savedRules = updatedRules

                            val token = spotifyAccessToken

                            if (rulesLoaded && !token.isNullOrBlank()) {
                                scope.launch {
                                    try {
                                        ruleStorage.saveRulesToBackend(
                                            accessToken = token,
                                            backendBaseUrl = backendBaseUrl,
                                            rules = updatedRules
                                        )
                                        Log.d("Cloudberry", "Rules saved to backend")
                                    } catch (error: Exception) {
                                        Log.e("Cloudberry", "Failed to save playlist rules to backend", error)
                                    }
                                }
                            }

                            ruleBeingEdited = null
                            onPageSelected("rules")
                        }
                    )
                }
            }
        }
    }
}

private fun detectSeason(currentTime: Calendar): String {
    return when (currentTime.get(Calendar.MONTH)) {
        Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> "Winter"
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> "Spring"
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> "Summer"
        Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> "Fall"
        else -> "Fall"
    }
}



private fun matchesPlaylistRule(
    rule: PlaylistRule,
    detectedSeason: String,
    currentTime: Calendar,
    isRaining: Boolean
): Boolean {
    // Rain is an additional requirement for a rule.
    if (rule.rainEnabled) {
        if (!isRaining) {
            return false
        }

        // Season-scoped rain only applies when the rule has a season.
        if (
            rule.rainScope.equals("season", ignoreCase = true) &&
            rule.season.isBlank()
        ) {
            return false
        }
    }

    val seasonMatches = rule.season.equals(
        detectedSeason,
        ignoreCase = true
    )

    val hour = currentTime.get(Calendar.HOUR_OF_DAY)
    val minute = currentTime.get(Calendar.MINUTE)

    val currentTimeIndex =
        hour * 2 + if (minute >= 30) 1 else 0

    val timeMatches = rule.timeRanges.any { range ->
        val start = range.first
        val end = range.second

        if (start < end) {
            currentTimeIndex in start until end
        } else if (start > end) {
            currentTimeIndex >= start || currentTimeIndex < end
        } else {
            currentTimeIndex == start
        }
    }

    return when (rule.triggerOption.lowercase()) {
        "season only" -> seasonMatches
        "time only" -> timeMatches
        "season and time" -> seasonMatches && timeMatches
        else -> false
    }
}

private suspend fun logRulePlaylistOrderAndTrackCounts(
    rule: PlaylistRule,
    accessToken: String
) {
    val tag = "CloudberryPlaylist"

    // Keep only playlists selected in this rule, using their IDs.
    val selectedIds = rule.playlists.map { it.id }.toSet()

    val orderedPlaylists = rule.playlistOrder
        .filter { it.id in selectedIds }
        .distinctBy { it.id }
        .let { ordered ->
            // Fall back to the selected playlist list if custom order is empty.
            val baseOrder = if (ordered.isNotEmpty()) {
                ordered
            } else {
                rule.playlists.distinctBy { it.id }
            }

            if (rule.randomOrder) baseOrder.shuffled() else baseOrder
        }


    // Fetch each playlist's track count from Spotify on a background thread.
    withContext(Dispatchers.IO) {
        for ((index, playlist) in orderedPlaylists.withIndex()) {
            try {
                val url = java.net.URL(
                    "https://api.spotify.com/v1/playlists/${playlist.id}?fields=id,name,items(total)"
                )

                val connection =
                    url.openConnection() as java.net.HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $accessToken"
                    )
                    connection.setRequestProperty("Accept", "application/json")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val status = connection.responseCode
                    val response = (
                            if (status in 200..299) connection.inputStream
                            else connection.errorStream
                            )?.bufferedReader()?.use { it.readText() }.orEmpty()

                    if (status in 200..299) {
                        val json =
                            com.google.gson.JsonParser.parseString(response).asJsonObject

                        val totalTracks = json
                            .getAsJsonObject("items")
                            ?.get("total")
                            ?.asInt

                        android.util.Log.d(
                            tag,
                            "Track count ${index + 1}/${orderedPlaylists.size}: " +
                                    "${playlist.name} = ${totalTracks ?: "unknown"} tracks"
                        )
                    } else {
                        android.util.Log.e(
                            tag,
                            "Failed to fetch ${playlist.name}: HTTP $status | $response"
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    tag,
                    "Error fetching track count for ${playlist.name}",
                    e
                )
            }
        }
    }
}


private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}