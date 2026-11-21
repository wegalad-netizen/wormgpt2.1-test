package com.wegalad.wormgpt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Warns about likely spam SMS. Android does not allow third-party deletion. */
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
            val body = bodyBuilder.toString()
            val numberHit = SpamPrefs.isBlocked(context, sender)
            val keywordHit = SpamPrefs.isSpamSms(context, body)
            if (!numberHit && !keywordHit) continue

            val reason = if (numberHit) "blocked number" else "spam keywords"
            val preview = body.replace(Regex("\\s+"), " ").take(140)
            SpamPrefs.addLog(context, "SMS   $sender  [$reason]  $preview")
            SpamNotifier.notify(
                context,
                "Spam SMS detected",
                "From $sender ($reason): $preview",
            )
        }
    }
}
