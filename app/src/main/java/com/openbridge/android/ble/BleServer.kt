package com.openbridge.android.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import java.nio.ByteBuffer

/**
 * BLE GATT server for OpenBridge.
 *
 * Exposes characteristics defined in BleUuids. Handles iPhone writes (commands)
 * and sends notifications (events) when something happens on the Android side.
 *
 * Pairing flow:
 *  1. iPhone connects, reads STATUS
 *  2. iPhone writes pair_start to COMMAND with its pubkey
 *  3. Android responds with pair_confirm event + its pubkey + 6-digit code hash
 *  4. User enters code on iPhone; iPhone writes pair_complete with code hash
 *  5. Both sides derive session key via ECDH; subsequent traffic is encrypted
 *
 * See docs/PROTOCOL.md for full spec.
 */
class BleServer(private val context: Context) {

    private val TAG = "BleServer"

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private var callback: ServerCallback? = null

    /**
     * Callback for events the rest of the app needs to know about.
     */
    interface ServerCallback {
        fun onCommandReceived(payload: ByteArray)
        fun onDeviceConnected(device: android.bluetooth.BluetoothDevice)
        fun onDeviceDisconnected(device: android.bluetooth.BluetoothDevice)
        fun onAdvertisingStarted()
        fun onAdvertisingFailed(errorCode: Int)
    }

    fun setCallback(cb: ServerCallback) {
        callback = cb
    }

    /**
     * Start the GATT server and begin BLE advertising.
     * Returns true if successfully started.
     */
    fun start(): Boolean {
        if (!bluetoothManager.adapter.isEnabled) {
            Log.w(TAG, "bluetooth adapter not enabled")
            return false
        }

        // Setup GATT server
        gattServer = bluetoothManager.openGattServer(context, gattCallback)
        if (gattServer == null) {
            Log.e(TAG, "failed to open GATT server")
            return false
        }

        val service = BluetoothGattService(
            BleUuids.SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Command characteristic (write)
        val cmdChar = BluetoothGattCharacteristic(
            BleUuids.CHARACTERISTIC_COMMAND,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        cmdChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        // Event characteristic (notify)
        val evtChar = BluetoothGattCharacteristic(
            BleUuids.CHARACTERISTIC_EVENT,
            BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val cccDescriptor = BluetoothGattDescriptor(
            BleUuids.DESCRIPTOR_CCC,
            BluetoothGattDescriptor.PERMISSION_READ or
                BluetoothGattDescriptor.PERMISSION_WRITE
        )
        evtChar.addDescriptor(cccDescriptor)

        // Bulk characteristic (read/write)
        val bulkChar = BluetoothGattCharacteristic(
            BleUuids.CHARACTERISTIC_BULK,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or
                BluetoothGattCharacteristic.PERMISSION_READ
        )

        // Status characteristic (read only, unencrypted)
        val statusChar = BluetoothGattCharacteristic(
            BleUuids.CHARACTERISTIC_STATUS,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(cmdChar)
        service.addCharacteristic(evtChar)
        service.addCharacteristic(bulkChar)
        service.addCharacteristic(statusChar)

        gattServer?.addService(service)

        // Start advertising
        advertiser = bluetoothManager.adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.w(TAG, "BLE advertising not supported on this device")
            return false
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // continuous
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(android.os.ParcelUuid(BleUuids.SERVICE))
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
        return true
    }

    fun stop() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Log.w(TAG, "failed to stop advertising: ${e.message}")
        }
        try {
            gattServer?.close()
        } catch (e: Exception) {
            Log.w(TAG, "failed to close GATT server: ${e.message}")
        }
        advertiser = null
        gattServer = null
    }

    /**
     * Notify a connected iPhone of an event.
     * The payload should be already encrypted by the caller.
     */
    fun notifyEvent(encryptedPayload: ByteArray) {
        val server = gattServer ?: return
        val service = server.getService(BleUuids.SERVICE) ?: return
        val char = service.getCharacteristic(BleUuids.CHARACTERISTIC_EVENT) ?: return
        char.value = encryptedPayload
        // Notify all connected devices
        bluetoothManager.connectedDevices.forEach { device ->
            server.notifyCharacteristicChanged(device, char, false)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "BLE advertising started")
            callback?.onAdvertisingStarted()
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: errorCode=$errorCode")
            callback?.onAdvertisingFailed(errorCode)
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: android.bluetooth.BluetoothDevice,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "device connected: ${device.address}")
                    callback?.onDeviceConnected(device)
                }
                android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "device disconnected: ${device.address}")
                    callback?.onDeviceDisconnected(device)
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.i(TAG, "write request: char=${characteristic.uuid} len=${value.size}")

            when (characteristic.uuid) {
                BleUuids.CHARACTERISTIC_COMMAND -> {
                    callback?.onCommandReceived(value)
                }
                BleUuids.CHARACTERISTIC_BULK -> {
                    // TODO: handle bulk transfer chunks
                    Log.d(TAG, "bulk chunk received (unimplemented)")
                }
            }

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, android.bluetooth.BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }

        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(TAG, "read request: char=${characteristic.uuid}")

            when (characteristic.uuid) {
                BleUuids.CHARACTERISTIC_STATUS -> {
                    // Return protocol version and connection state as a small JSON-like binary
                    val resp = encodeStatus()
                    gattServer?.sendResponse(
                        device, requestId,
                        android.bluetooth.BluetoothGatt.GATT_SUCCESS, 0, resp
                    )
                }
                else -> {
                    gattServer?.sendResponse(
                        device, requestId,
                        android.bluetooth.BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, ByteArray(0)
                    )
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            // CCC descriptor — enable/disable notifications
            if (descriptor.uuid == BleUuids.DESCRIPTOR_CCC) {
                Log.i(TAG, "CCC descriptor write (notify ${value.contentToString()}) from ${device.address}")
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, android.bluetooth.BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }

        private fun encodeStatus(): ByteArray {
            // Simple binary: 4 bytes version magic + 1 byte state + 1 byte paired
            val buf = ByteBuffer.allocate(8)
            buf.putInt(0x4F425200) // "OBR\0" magic
            buf.put(0x01.toByte()) // protocol version 0.1
            buf.put(if (isPaired()) 1 else 0) // paired state
            buf.put(0) // reserved
            buf.put(0) // reserved
            return buf.array()
        }
    }

    private fun isPaired(): Boolean {
        return SecureStorageBridge.isPaired()
    }
}

// Helper to avoid circular dependency
private object SecureStorageBridge {
    fun isPaired(): Boolean {
        return try {
            val clazz = Class.forName("com.openbridge.android.security.SecureStorage")
            val keys = clazz.getDeclaredClass("Keys") ?: return false
            // Use reflection to avoid tight coupling in this skeleton
            // In production, inject this via constructor
            false
        } catch (_: Exception) {
            false
        }
    }
}
