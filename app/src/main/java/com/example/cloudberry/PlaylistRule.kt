package com.example.cloudberry

data class PlaylistRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val playlists: List<SpotifyPlaylist>,
    val playlistOrder: List<SpotifyPlaylist>,
    val randomOrder: Boolean,
    val triggerOption: String,
    val season: String = "",
    val rainEnabled: Boolean = false,
    val rainScope: String = "season",
    val timeRanges: List<Pair<Int, Int>> = emptyList()
)