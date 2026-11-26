package com.openbridge.android.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.openbridge.android.R
import com.openbridge.android.ble.BleServer
import com.openbridge.android.security.AuditLog
import com.openbridge.android.security.SecureStorage
import com.openbridge.android.telephony.TelephonyBridge

/**
 * BridgeService is the foreground service that runs while OpenBridge is active.
 *
 * It owns:
 *  - BleServer: GATT server for iPhone communication
 *  - TelephonyBridge: SMS/Contacts/CallLog access
 *
 * The service runs as foreground (with a persistent notification) so that
 * Android keeps it alive while the phone is paired with the iPhone.
 */
class BridgeService : Service() {

    private val TAG = "BridgeService"

    companion object {
        const val CHANNEL_ID = "openbridge_bridge"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.openbridge.android.action.START"
        const val ACTION_STOP = "com.openbridge.android.action.STOP"
        const val ACTION_SEND_SMS = "com.openbridge.android.action.SEND_SMS"
        const val EXTRA_PHONE = "phone"
        const val EXTRA_BODY = "body"
    }

    private val binder = LocalBinder()
    private lateinit var bleServer: BleServer
    private lateinit var telephonyBridge: TelephonyBridge

    inner class LocalBinder : Binder() {
        fun getService(): BridgeService = this@BridgeService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "BridgeService created")
        bleServer = BleServer(this)
        telephonyBridge = TelephonyBridge(this)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopBridge()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SEND_SMS -> {
                val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                if (phone.isNotEmpty()) {
                    Log.i(TAG, "send SMS via intent, peer_len=${phone.length}")
                    telephonyBridge.sendSms(phone, body)
                }
            }
            else -> {
                startBridge()
            }
        }
        return START_STICKY
    }

    private fun startBridge() {
        Log.i(TAG, "starting bridge")
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)

        AuditLog.record("bridge_started")

        bleServer.setCallback(object : BleServer.ServerCallback {
            override fun onCommandReceived(payload: ByteArray) {
                Log.i(TAG, "command received, len=${payload.size}")
                // TODO: decrypt and parse command, dispatch to telephonyBridge
                AuditLog.record("ble_command_received", mapOf("payload_len" to payload.size))
            }
            override fun onDeviceConnected(device: android.bluetooth.BluetoothDevice) {
                AuditLog.record("ble_device_connected", mapOf("device_hash" to device.address.take(8)))
                updateNotification(true)
            }
            override fun onDeviceDisconnected(device: android.bluetooth.BluetoothDevice) {
                AuditLog.record("ble_device_disconnected", mapOf("device_hash" to device.address.take(8)))
                updateNotification(false)
            }
            override fun onAdvertisingStarted() {
                Log.i(TAG, "BLE advertising started")
            }
            override fun onAdvertisingFailed(errorCode: Int) {
                Log.e(TAG, "BLE advertising failed: $errorCode")
            }
        })
        bleServer.start()
    }

    private fun stopBridge() {
        Log.i(TAG, "stopping bridge")
        AuditLog.record("bridge_stopped")
        bleServer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "BridgeService destroyed")
        stopBridge()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bridge_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.bridge_channel_desc)
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(connected: Boolean = false): Notification {
        val title = getString(R.string.bridge_service_title)
        val body = if (connected) getString(R.string.bridge_service_body)
                   else getString(R.string.bridge_service_body_disconnected)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(connected: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(connected))
    }
}
