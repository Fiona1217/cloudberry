
package com.example.cloudberry

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.AEADBadTagException

class SpotifyTokenStorage(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "SpotifyTokenStorage"
        private const val PREFS_NAME = "spotify_secure_tokens"
        private const val MASTER_KEY_ALIAS = "com.example.cloudberry.spotify.master_key"
    }

    private val preferences = createPreferences()

    private fun createPreferences() =
        try {
            buildEncryptedPreferences()
        } catch (e: Exception) {

            if (!isDecryptionFailure(e)) {
                throw e
            }

            Log.w(TAG, "Encrypted token storage is corrupted. Resetting it.", e)
            appContext.deleteSharedPreferences(PREFS_NAME)

            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
            } catch (keyError: Exception) {
                Log.e(TAG, "Could not reset Spotify encryption key.", keyError)
            }
            buildEncryptedPreferences()
        }

    private fun buildEncryptedPreferences() =
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            MasterKey.Builder(appContext, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private fun isDecryptionFailure(error: Throwable): Boolean {
        var current: Throwable? = error

        while (current != null) {
            if (current is AEADBadTagException ||
                current.message?.contains(
                    "Signature/MAC verification failed",
                    ignoreCase = true
                ) == true
            ) {
                return true
            }

            current = current.cause
        }
        return false
    }

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
