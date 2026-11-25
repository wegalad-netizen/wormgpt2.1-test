package com.wegalad.wormgpt

import android.telecom.Call
import android.telecom.CallScreeningService

/** Rejects incoming calls from the user's blocklist or operator caller IDs. Never places calls. */
class SpamCallScreeningService : CallScreeningService() {

    private val operatorMarkers = listOf(
        "HUTCH", "HUTCHISON", "MOBITEL", "SLT", "DIALOG", "AIRTEL",
    )

    private val scamCallerMarkers = listOf(
        "spam", "fraud", "scam", "telemarketing", "suspected spam",
        "possible spam", "caller verified", "unknown caller",
    )

    override fun onScreenCall(callDetails: Call.Details) {
        if (!SpamPrefs.callGuardEnabled(this)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val displayName = callDetails.callerDisplayName.orEmpty()
        val hidden = number.isBlank()
        if (SpamPrefs.isAllowed(this, number)) {
            val allowedLabel = number.ifBlank { displayName }.ifBlank { "Hidden / unknown" }
            respondToCall(callDetails, CallResponse.Builder().build())
            SpamPrefs.recordScreenedCall(this, allowedLabel, false)
            SpamPrefs.addRecentCall(this, allowedLabel, false)
            return
        }

        val operatorSpam = SpamPrefs.operatorSpamEnabled(this) && (
            operatorMarkers.any { displayName.contains(it, ignoreCase = true) } ||
                operatorMarkers.any { number.contains(it, ignoreCase = true) } ||
                scamCallerMarkers.any { displayName.contains(it, ignoreCase = true) }
            )
        val blocked = SpamPrefs.isBlocked(this, number) ||
            (hidden && SpamPrefs.blockUnknown(this)) ||
            operatorSpam

        val builder = CallResponse.Builder().apply {
            if (blocked) {
                setDisallowCall(true)
                setRejectCall(true)
                setSkipCallLog(true)
                setSkipNotification(true)
            }
        }
        respondToCall(callDetails, builder.build())

        val label = when {
            number.isNotBlank() && displayName.isNotBlank() -> "$number · $displayName"
            number.isNotBlank() -> number
            displayName.isNotBlank() -> displayName
            else -> "Hidden / unknown"
        }
        SpamPrefs.recordScreenedCall(this, label, blocked)
        SpamPrefs.addRecentCall(this, label, blocked)
        if (blocked) {
            SpamPrefs.addLog(this, "CALL  $label")
            SpamNotifier.notify(this, "Spam call blocked", "Rejected a call from $label")
        }
    }
}
