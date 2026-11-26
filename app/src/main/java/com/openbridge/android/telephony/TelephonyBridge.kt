package com.openbridge.android.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.openbridge.android.security.AuditLog

/**
 * Bridges SMS/Contacts/CallLog between Android system providers and the BLE/Bridge layer.
 *
 * All accesses are logged to the AuditLog (metadata only, never content).
 */
class TelephonyBridge(private val context: Context) {

    private val TAG = "TelephonyBridge"

    // ===== SMS =====

    /**
     * Send an SMS message.
     *
     * @param to Recipient phone number.
     * @param body Message body.
     * @return SMS provider message ID on success, null on failure.
     */
    @SuppressLint("MissingPermission")
    fun sendSms(to: String, body: String): String? {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            Log.w(TAG, "sendSms: SEND_SMS permission not granted")
            return null
        }

        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(to, null, body, null, null)

            // Record audit log (metadata only)
            AuditLog.record(
                "sms_sent",
                mapOf(
                    "peer_len" to to.length,
                    "body_len" to body.length,
                    "peer_hash" to hash(to)
                )
            )
            Log.i(TAG, "SMS sent to len=${to.length} body_len=${body.length}")

            // Generate a pseudo ID (real impl would track via content provider)
            "sms_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Log.e(TAG, "sendSms failed: ${e.message}")
            AuditLog.record("sms_failed", mapOf("peer_len" to to.length, "error" to (e.message ?: "")))
            null
        }
    }

    /**
     * Read recent SMS conversations (max 50).
     * Returns a list of maps with from, body, date, type.
     */
    @SuppressLint("MissingPermission")
    fun readSmsHistory(limit: Int = 50): List<Map<String, Any>> {
        if (!hasPermission(Manifest.permission.READ_SMS)) {
            Log.w(TAG, "readSmsHistory: READ_SMS permission not granted")
            return emptyList()
        }

        val result = mutableListOf<Map<String, Any>>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        ) ?: return emptyList()

        cursor.use {
            AuditLog.record("sms_history_access", mapOf("limit" to limit))
            while (it.moveToNext()) {
                result.add(mapOf(
                    "from" to (it.getString(0) ?: ""),
                    "body" to (it.getString(1) ?: ""),
                    "date" to (it.getLong(2)),
                    "type" to (it.getInt(3))
                ))
            }
        }
        Log.i(TAG, "read ${result.size} SMS records")
        return result
    }

    // ===== Contacts =====

    /**
     * Read contacts (max 500 by default).
     * READ-ONLY — OpenBridge does NOT have WRITE_CONTACTS permission.
     */
    @SuppressLint("MissingPermission")
    fun readContacts(limit: Int = 500): List<Map<String, String>> {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            Log.w(TAG, "readContacts: READ_CONTACTS permission not granted")
            return emptyList()
        }

        val result = mutableListOf<Map<String, String>>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC LIMIT $limit"
        ) ?: return emptyList()

        cursor.use { c ->
            AuditLog.record("contacts_access", mapOf("limit" to limit))
            val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIdx = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (c.moveToNext()) {
                val id = c.getString(idIdx)
                val name = c.getString(nameIdx) ?: ""
                val hasPhone = c.getInt(hasPhoneIdx) > 0
                var phone = ""
                if (hasPhone) {
                    phone = readFirstPhoneForContact(id)
                }
                result.add(mapOf(
                    "id" to id,
                    "name" to name,
                    "phone" to phone
                ))
            }
        }
        Log.i(TAG, "read ${result.size} contacts")
        return result
    }

    private fun readFirstPhoneForContact(contactId: String): String {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            "LIMIT 1"
        ) ?: return ""
        cursor.use {
            if (it.moveToFirst()) return it.getString(0) ?: ""
        }
        return ""
    }

    // ===== Call Log =====

    @SuppressLint("MissingPermission")
    fun readCallLog(limit: Int = 50): List<Map<String, Any>> {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) {
            Log.w(TAG, "readCallLog: READ_CALL_LOG permission not granted")
            return emptyList()
        }

        val result = mutableListOf<Map<String, Any>>()
        val projection = arrayOf(
            android.provider.CallLog.Calls.NUMBER,
            android.provider.CallLog.Calls.DATE,
            android.provider.CallLog.Calls.DURATION,
            android.provider.CallLog.Calls.TYPE
        )

        val cursor = context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${android.provider.CallLog.Calls.DATE} DESC LIMIT $limit"
        ) ?: return emptyList()

        cursor.use { c ->
            AuditLog.record("call_log_access", mapOf("limit" to limit))
            while (c.moveToNext()) {
                result.add(mapOf(
                    "number" to (c.getString(0) ?: ""),
                    "date" to c.getLong(1),
                    "duration" to c.getLong(2),
                    "type" to c.getInt(3)
                ))
            }
        }
        Log.i(TAG, "read ${result.size} call log records")
        return result
    }

    // ===== Helpers =====

    private fun hasPermission(perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    private fun hash(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }
}
