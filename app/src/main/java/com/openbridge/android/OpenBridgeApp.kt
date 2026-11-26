package com.openbridge.android

import android.app.Application
import android.util.Log
import com.openbridge.android.security.AuditLog
import com.openbridge.android.security.SecureStorage

/**
 * OpenBridge Application class.
 *
 * Initializes security-sensitive components early in the app lifecycle.
 * No telemetry, no analytics, no crash reporters — by design.
 */
class OpenBridgeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "OpenBridge v${BuildConfig.VERSION_NAME} starting")

        // Initialize EncryptedSharedPreferences
        SecureStorage.init(this)

        // Initialize audit log (disabled by default)
        AuditLog.init(this)
    }

    companion object {
        private const val TAG = "OpenBridgeApp"
    }
}
