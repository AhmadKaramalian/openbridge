package com.openbridge.android.ble

import java.util.UUID

/**
 * BLE GATT UUIDs for OpenBridge.
 *
 * These are NEW UUIDs generated specifically for OpenBridge v0.1.
 * They are intentionally different from any other app's UUIDs.
 *
 * Generated via:
 *   uuidgen --sha1 --namespace @dns --name "openbridge-v0.example.org"
 * (or equivalent v5 UUID in DNS namespace)
 *
 * Service UUID: 8f3c1a00-5b8f-4c2a-9d3e-2f6a1b0c9d01
 *   Characteristics use the same prefix with sequential suffix.
 */
object BleUuids {

    /** Main OpenBridge GATT service. */
    val SERVICE: UUID = UUID.fromString("8f3c1a00-5b8f-4c2a-9d3e-2f6a1b0c9d01")

    /** Write characteristic: iPhone → Android commands (encrypted). */
    val CHARACTERISTIC_COMMAND: UUID = UUID.fromString("8f3c1a01-5b8f-4c2a-9d3e-2f6a1b0c9d01")

    /** Notify characteristic: Android → iPhone events (encrypted). */
    val CHARACTERISTIC_EVENT: UUID = UUID.fromString("8f3c1a02-5b8f-4c2a-9d3e-2f6a1b0c9d01")

    /** Bulk transfer characteristic: large payloads (contact sync). */
    val CHARACTERISTIC_BULK: UUID = UUID.fromString("8f3c1a03-5b8f-4c2a-9d3e-2f6a1b0c9d01")

    /** Read-only status characteristic: connection state, version (unencrypted). */
    val CHARACTERISTIC_STATUS: UUID = UUID.fromString("8f3c1a04-5b8f-4c2a-9d3e-2f6a1b0c9d01")

    /** Standard Client Characteristic Configuration Descriptor (for enable notify). */
    val DESCRIPTOR_CCC: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
