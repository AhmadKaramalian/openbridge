package com.openbridge.android.call

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.openbridge.android.security.AuditLog

/**
 * OpenBridge InCallService.
 *
 * OPTIONAL — user must enable in Settings. Default: OFF.
 *
 * When enabled, takes over the call UI to:
 *  - Notify iPhone of incoming calls
 *  - Allow iPhone to answer/reject
 *  - Route audio to iPhone
 *
 * Unlike the main BridgeService, this is bound by the Telecom system
 * when a call is active, not by us.
 */
class OpenBridgeInCallService : InCallService() {

    private val TAG = "InCallService"

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.i(TAG, "call added: state=${call.state}")
        AuditLog.record(
            "call_incoming_or_outgoing",
            mapOf(
                "state" to call.state,
                "start_time" to System.currentTimeMillis()
            )
        )
        // TODO: notify iPhone via BleServer
        // TODO: register call callback for state changes
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(TAG, "call removed: state=${call.state}")
        AuditLog.record(
            "call_ended",
            mapOf("state" to call.state)
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "InCallService bound")
    }

    override fun onDestroy() {
        Log.i(TAG, "InCallService unbound")
        super.onDestroy()
    }
}
