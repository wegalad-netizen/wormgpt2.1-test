package com.wegalad.wormgpt

import android.app.Notification
import android.os.Bundle
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Cancels spam SMS notifications posted by the user's messaging app.
 *
 * Android does not let a normal app delete an SMS, so the message stays in the
 * inbox while its notification is hidden. Notification access is required.
 */
class SpamSmsNotificationListener : NotificationListenerService() {

    private val knownSmsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.samsung.android.messaging.rcs",
        "com.oneplus.mms",
        "com.oplus.mms",
        "com.coloros.mms",
        "com.miui.smsextra",
        "com.textra",
        "com.hellotext.hello",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return
        if (!SpamPrefs.smsGuardEnabled(this)) return
        val hideSpam = SpamPrefs.hideSpamSms(this)
        val hideOtp = SpamPrefs.hideOtpSms(this)
        if (!hideSpam && !hideOtp) return
        if (!isSmsNotification(sbn, notification)) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()
            .trim()
        val body = extractBody(extras)
        if (body.isBlank()) return

        val sender = title.ifBlank { body.substringBefore(":").trim().take(32) }
        if (SpamPrefs.isAllowed(this, sender)) return
        val otpHit = hideOtp && SpamPrefs.isOneTimeCodeMessage(body)
        val numberHit = hideSpam && SpamPrefs.isBlocked(this, sender)
        val keywordHit = hideSpam && SpamPrefs.isSpamSms(this, body, sender)
        if (!otpHit && !numberHit && !keywordHit) return

        cancelNotification(sbn.key)

        if (otpHit) {
            // OTP codes are sensitive; log only that an alert was hidden, never the message body/code.
            SpamPrefs.addLog(this, "OTP SMS ALERT HIDDEN  $sender")
            return
        }
        val reason = if (numberHit) "blocked number" else "spam keywords"
        val preview = body.replace(Regex("\\s+"), " ").take(140)
        SpamPrefs.addLog(this, "SMS HIDDEN  $sender  [$reason]  $preview")
        if (!numberHit && keywordHit) {
            // Spam verdict that is not on the blocklist: quarantine for triage.
            SpamPrefs.addQuarantine(
                this,
                QuarantineItem(
                    sender = sender.ifBlank { "Unknown" },
                    preview = preview,
                    score = SpamPrefs.spamScore(this, body, sender),
                    verdict = "SPAM",
                    created = System.currentTimeMillis(),
                    signals = SpamPrefs.spamSignals(this, body, sender),
                ),
            )
        }
    }

    private fun isSmsNotification(
        sbn: StatusBarNotification,
        notification: Notification,
    ): Boolean {
        if (sbn.packageName in knownSmsPackages) return true
        val defaultSms = Telephony.Sms.getDefaultSmsPackage(this)
        if (!defaultSms.isNullOrBlank() && sbn.packageName == defaultSms) return true
        // Some OEM SMS apps do not use a known package name but still tag
        // message notifications. Only trust the category for SMS-looking apps.
        val packageName = sbn.packageName.lowercase()
        return notification.category == Notification.CATEGORY_MESSAGE &&
            (packageName.contains("sms") || packageName.contains("mms"))
    }

    private fun extractBody(extras: Bundle): String {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString() }
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.joinToString(" ") { item ->
                (item as? Bundle)?.getCharSequence("text")?.toString().orEmpty()
            }
        return listOf(bigText, text, lines, messages)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
    }
}
