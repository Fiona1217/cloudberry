
package com.example.cloudberry

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider

private val SpotifyGreen = Color(0xFF1DB954)
private val LogoutRed = Color(0xFFD93025)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean,
    isConnecting: Boolean,
    isAuthorizing: Boolean,
    spotifyUsername: String = "",
    spotifyProfilePicture: Bitmap? = null,
    weatherCity: String = "",
    weatherCountry: String = "",
    weatherTemperature: Double? = null,
    weatherCondition: String = "",
    isRaining: Boolean = false,
    weatherStatus: String = "Weather unavailable",
    season: String = "",
    currentTime: String = "",
    onLoginLogout: () -> Unit
) {
    val ruleText = Color(0xFF49382D)
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "settings & info",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = ruleText
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
//                    .padding(10.dp)
            ) {
                // Row 2: Icon/PFP | Account details | Action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Column 1: Profile picture or Spotify music icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoggedIn && spotifyProfilePicture != null) {
                            Image(
                                bitmap = spotifyProfilePicture.asImageBitmap(),
                                contentDescription = "Spotify profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Spotify",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Column 2: Username and status
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isLoggedIn) {
                                spotifyUsername.ifBlank { "spotify account" }
                            } else {
                                "spotify"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Text(
                            text = when {
                                isConnecting -> "connecting..."
                                isAuthorizing -> "authorizing..."
                                isLoggedIn -> "logged in"
                                else -> "not logged in"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Column 3: Login or logout button
                    if (isLoggedIn) {
                        Button(
                            onClick = { showLogoutDialog = true },
                            enabled = !isConnecting && !isAuthorizing,
                            colors = buttonColors(
                                containerColor = LogoutRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Text(
                                text = "logout",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onLoginLogout,
                            enabled = !isConnecting && !isAuthorizing,
                            colors = buttonColors(
                                containerColor = Color(0xFF49382D),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Text(
                                text = when {
                                    isConnecting -> "Connecting..."
                                    isAuthorizing -> "Opening..."
                                    else -> "login"
                                },
                                color = SpotifyGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "current conditions",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ruleText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Weather
                Column {
                    Text(
                        text = "weather",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF896B52)
                    )

                    Text(
                        text = when {
                            weatherTemperature != null &&
                                    weatherCondition.isNotBlank() -> {
                                "$weatherCondition · ${weatherTemperature}°C"
                            }

                            else -> weatherStatus
                        },
                        color = Color(0xFF896B52),
                        fontSize = 14.sp
                    )

                    if (weatherCity.isNotBlank()) {
                        Text(
                            text = listOf(weatherCity, weatherCountry)
                                .filter { it.isNotBlank() }
                                .joinToString(", "),
                            color = Color(0xFF896B52),
                            fontSize = 12.sp
                        )
                    }

                    if (weatherTemperature != null) {
                        Text(
                            text = if (isRaining) {
                                "rain detected"
                            } else {
                                "no rain detected"
                            },
                            color = Color(0xFF896B52),
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFEADFD2)
                )

                // Season
                Column {
                    Text(
                        text = "season",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF896B52)
                    )

                    Text(
                        text = season.ifBlank { "unavailable" },
                        color = Color(0xFF896B52),
                        fontSize = 14.sp
                    )
                }

                HorizontalDivider(
                    color = Color(0xFFEADFD2)
                )

                // Local time
                Column {
                    Text(
                        text = "local time",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF896B52)
                    )

                    Text(
                        text = currentTime.ifBlank { "--:--" },
                        color = Color(0xFF896B52),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "log out of spotify?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("are you sure you want to log out of your spotify account?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLoginLogout()
                    },
                    colors = buttonColors(
                        containerColor = LogoutRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "logout",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("cancel")
                }
            }
        )
    }
}
