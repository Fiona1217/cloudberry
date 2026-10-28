
package com.example.cloudberry

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SpotifyTokenStorage(context: Context) {

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "spotify_secure_tokens",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveRefreshToken(token: String) {
        preferences.edit()
            .putString("refresh_token", token)
            .apply()
    }

    fun getRefreshToken(): String? {
        return preferences.getString("refresh_token", null)
    }

    fun clearTokens() {
        preferences.edit().clear().apply()
    }
}
