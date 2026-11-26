package com.openbridge.android.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper around EncryptedSharedPreferences.
 *
 * All sensitive data (pairing state, audit log key, etc.) goes through this class.
 * Data is encrypted at rest with a hardware-backed key (when available).
 *
 * Non-sensitive data (UI preferences like dark mode) uses regular SharedPreferences.
 */
object SecureStorage {

    private const val PREFS_NAME = "openbridge_secure"
    private const val PREFS_NAME_PLAIN = "openbridge_ui"

    private lateinit var encryptedPrefs: SharedPreferences
    private lateinit var plainPrefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        plainPrefs = context.getSharedPreferences(PREFS_NAME_PLAIN, Context.MODE_PRIVATE)
    }

    // ===== Encrypted getters (for sensitive data) =====

    fun putEncrypted(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getEncrypted(key: String, default: String = ""): String =
        encryptedPrefs.getString(key, default) ?: default

    fun putEncryptedBytes(key: String, value: ByteArray) {
        encryptedPrefs.edit().putString(key, value.toHex()).apply()
    }

    fun getEncryptedBytes(key: String, default: ByteArray = ByteArray(0)): ByteArray =
        encryptedPrefs.getString(key, null)?.fromHex() ?: default

    fun removeEncrypted(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }

    fun clearEncrypted() {
        encryptedPrefs.edit().clear().apply()
    }

    // ===== Plain getters (for non-sensitive UI prefs) =====

    fun putPlain(key: String, value: Boolean) {
        plainPrefs.edit().putBoolean(key, value).apply()
    }

    fun getPlain(key: String, default: Boolean = false): Boolean =
        plainPrefs.getBoolean(key, default)

    fun putPlain(key: String, value: String) {
        plainPrefs.edit().putString(key, value).apply()
    }

    fun getPlain(key: String, default: String = ""): String =
        plainPrefs.getString(key, default) ?: default

    fun putPlain(key: String, value: Int) {
        plainPrefs.edit().putInt(key, value).apply()
    }

    fun getPlain(key: String, default: Int = 0): Int =
        plainPrefs.getInt(key, default)

    // ===== String <-> ByteArray helpers =====

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    // ===== Pairing state keys =====

    object Keys {
        const val PAIRED_IPHONE_PUBKEY = "paired_iphone_pubkey"
        const val PAIRED_SESSION_KEY = "paired_session_key"
        const val PAIRED_DEVICE_NAME = "paired_device_name"
        const val PAIRED_AT = "paired_at"
        const val SETUP_DONE = "setup_done"
        const val INCALL_ENABLED = "incall_enabled"
        const val AUDIT_ENABLED = "audit_enabled"
        const val LAST_SEQUENCE = "last_sequence"
    }
}
