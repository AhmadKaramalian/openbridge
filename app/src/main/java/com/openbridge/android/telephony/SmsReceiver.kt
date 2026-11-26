package com.openbridge.android.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.openbridge.android.security.AuditLog

/**
 * Receives incoming SMS broadcasts and notifies the bridge layer.
 *
 * Note: This is registered in the manifest with
 * android:permission="android.permission.BROADCAST_SMS" which means
 * only the system can deliver broadcasts to it.
 */
class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    override fun onReceive(context: Context, signalData: Intent) {
        if (signalData.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(signalData)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "received SMS_RECEIVED but no messages")
            return
        }

        for (msg in messages) {
            handleSms(context, msg)
        }
    }

    private fun handleSms(context: Context, msg: SmsMessage) {
        val from = msg.displayOriginatingAddress ?: msg.originatingAddress ?: "unknown"
        val body = msg.displayMessageBody ?: msg.messageBody ?: ""
        val ts = msg.timestampMillis

        Log.i(TAG, "SMS received from len=${from.length} body_len=${body.length}")

        // Record audit log (metadata only)
        AuditLog.record(
            "sms_received",
            mapOf(
                "peer_len" to from.length,
                "body_len" to body.length,
                "peer_hash" to hash(from),
                "ts" to ts
            )
        )

        // TODO: Forward to BleServer for delivery to iPhone
        // This will be done via a bound service or local broadcast once BridgeService is running
    }

    private fun hash(s: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
        } catch (_: Exception) {
            "0"
        }
    }
}
