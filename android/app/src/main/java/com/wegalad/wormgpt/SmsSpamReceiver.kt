package com.wegalad.wormgpt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationManagerCompat

/**
 * Detects likely spam SMS and posts an alert. Notification hiding is handled by
 * SpamSmsNotificationListener because Android does not allow third-party deletion.
 */
class SmsSpamReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!SpamPrefs.smsGuardEnabled(context)) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val bySender = LinkedHashMap<String, StringBuilder>()
        for (message in messages) {
            val sender = message.displayOriginatingAddress ?: continue
            bySender.getOrPut(sender) { StringBuilder() }
                .append(message.messageBody.orEmpty())
        }

        for ((sender, bodyBuilder) in bySender) {
            if (SpamPrefs.isAllowed(context, sender)) continue
            val body = bodyBuilder.toString()
            val numberHit = SpamPrefs.isBlocked(context, sender)
            val keywordHit = SpamPrefs.isSpamSms(context, body, sender)
            if (!numberHit && !keywordHit) continue

            val preview = body.replace(Regex("\\s+"), " ").take(140)

            if (numberHit) {
                SpamPrefs.addLog(context, "SMS   $sender  [blocked number]  $preview")
                // When hiding is active the listener removes the SMS notification,
                // so do not replace it with a Sentinal alert.
                val hiding = SpamPrefs.hideSpamSms(context) &&
                    NotificationManagerCompat.getEnabledListenerPackages(context)
                        .contains(context.packageName)
                if (!hiding) {
                    SpamNotifier.notify(
                        context,
                        "Spam SMS detected",
                        "From $sender (blocked number): $preview",
                    )
                }
            } else {
                // Spam verdict that is not on the blocklist: quarantine for triage.
                val score = SpamPrefs.spamScore(context, body, sender)
                val signals = SpamPrefs.spamSignals(context, body, sender)
                SpamPrefs.addQuarantine(
                    context,
                    QuarantineItem(
                        sender = sender.ifBlank { "Unknown" },
                        preview = preview,
                        score = score,
                        verdict = "SPAM",
                        created = System.currentTimeMillis(),
                        signals = signals,
                    ),
                )
                SpamPrefs.addLog(context, "SMS   $sender  [quarantined]  $preview")
                val hiding = SpamPrefs.hideSpamSms(context) &&
                    NotificationManagerCompat.getEnabledListenerPackages(context)
                        .contains(context.packageName)
                if (!hiding) {
                    SpamNotifier.notify(
                        context,
                        "Spam SMS detected",
                        "From $sender (quarantined): $preview",
                    )
                }
            }
        }
    }
}
