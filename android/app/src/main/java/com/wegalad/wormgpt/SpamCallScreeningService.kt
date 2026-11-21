package com.wegalad.wormgpt

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat

/** Rejects incoming calls from the user's blocklist. Never places calls. */
class SpamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (!SpamPrefs.callGuardEnabled(this)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val hidden = number.isBlank()
        val blocked = SpamPrefs.isBlocked(this, number) ||
            (hidden && SpamPrefs.blockUnknown(this))

        val builder = CallResponse.Builder().apply {
            if (blocked) {
                setDisallowCall(true)
                setSkipCallLog(true)
                setSkipNotification(true)
                if (ContextCompat.checkSelfPermission(
                        this@SpamCallScreeningService,
                        Manifest.permission.ANSWER_PHONE_CALLS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    setRejectCall(true)
                }
            }
        }
        respondToCall(callDetails, builder.build())

        if (blocked) {
            val label = number.ifBlank { "Hidden / unknown" }
            SpamPrefs.addLog(this, "CALL  $label")
            SpamNotifier.notify(this, "Spam call blocked", "Rejected a call from $label")
        }
    }
}
