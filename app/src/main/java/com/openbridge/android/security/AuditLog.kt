package com.openbridge.android.security

import android.content.Context
import android.util.Log
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Append-only, opt-in audit log.
 *
 * Records METADATA of sensitive operations only — never content.
 * For example: "sms_sent" records the timestamp and peer number length,
 * but NOT the message body.
 *
 * Stored in app-private storage. Never sent anywhere. Exportable by user.
 */
object AuditLog {

    private const val TAG = "AuditLog"
    private const val FILENAME = "audit_log.jsonl"
    private const val MAX_ENTRIES = 10_000

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    private lateinit var logFile: File
    private var enabled: Boolean = false

    fun init(context: Context) {
        logFile = File(context.filesDir, FILENAME)
        enabled = SecureStorage.getPlain(SecureStorage.Keys.AUDIT_ENABLED, false)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        SecureStorage.putPlain(SecureStorage.Keys.AUDIT_ENABLED, enabled)
        Log.i(TAG, "audit log ${if (enabled) "enabled" else "disabled"}")
    }

    fun isEnabled(): Boolean = enabled

    /**
     * Record an event. Only metadata is stored, never content.
     *
     * @param operation One of: sms_sent, sms_received, contact_access, call_incoming,
     *                  call_outgoing, ble_command, pairing_started, pairing_completed,
     *                  pairing_reset
     * @param metadata Key-value pairs of metadata. Content fields (message body, contact name)
     *                  MUST NOT be included — only lengths, hashes, types.
     */
    fun record(operation: String, metadata: Map<String, Any> = emptyMap()) {
        if (!enabled) return
        try {
            val entry = JSONObject().apply {
                put("ts", System.currentTimeMillis())
                put("ts_iso", dateFormat.format(Date()))
                put("op", operation)
                val meta = JSONObject()
                metadata.forEach { (k, v) ->
                    when (v) {
                        is String -> meta.put(k, sanitize(v))
                        is Number -> meta.put(k, v)
                        is Boolean -> meta.put(k, v)
                        else -> meta.put(k, v.toString())
                    }
                }
                put("meta", meta)
            }
            synchronized(this) {
                logFile.appendText(entry.toString() + "\n")
                // Rotate if too large
                if (logFile.length() > MAX_ENTRIES * 200L) { // estimate ~200 bytes per entry
                    rotate()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to record audit entry: ${e.message}")
        }
    }

    /**
     * Sanitize a string value: keep only length and first 2 chars (for type detection),
     * hash the rest.
     */
    private fun sanitize(value: String): String {
        if (value.length <= 4) return "len=${value.length}"
        return "len=${value.length},prefix=${value.take(2)},hash=${value.sha256().take(8)}"
    }

    private fun String.sha256(): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun rotate() {
        val rotated = File(logFile.parentFile, "$FILENAME.1")
        rotated.delete()
        logFile.renameTo(rotated)
        logFile.createNewFile()
    }

    /**
     * Export the audit log as JSON array. Suitable for sharing to file/email.
     */
    fun export(): JSONArray {
        val result = JSONArray()
        if (!logFile.exists()) return result
        logFile.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    try {
                        result.put(JSONObject(line))
                    } catch (_: Exception) {
                        // Skip malformed lines
                    }
                }
            }
        }
        return result
    }

    /**
     * Export to a stream (for "Share to..." feature).
     */
    fun exportTo(writer: OutputStreamWriter) {
        if (!logFile.exists()) {
            writer.write("[]")
            return
        }
        val arr = export()
        writer.write(arr.toString(2))
        writer.flush()
    }

    /**
     * Clear all audit log entries.
     */
    fun clear() {
        synchronized(this) {
            logFile.writeText("")
            val rotated = File(logFile.parentFile, "$FILENAME.1")
            rotated.delete()
        }
        Log.i(TAG, "audit log cleared")
    }

    /**
     * Get a count of entries (for UI display).
     */
    fun count(): Int {
        if (!logFile.exists()) return 0
        return logFile.useLines { it.count { l -> l.isNotBlank() } }
    }
}
