package com.wegalad.wormgpt

import android.content.Context
import android.content.SharedPreferences

/**
 * Shared storage for Spam Guard. The activity, call screener and SMS receiver
 * all read the same settings so the UI controls actually affect blocking.
 */
object SpamPrefs {
    private const val PREFS = "wormgpt_spam_prefs"
    private const val KEY_BLOCKED = "blocked_numbers"
    private const val KEY_KEYWORDS = "keyword_filter"
    private const val KEY_UNKNOWN = "block_unknown"
    private const val KEY_OPERATOR_SPAM = "operator_spam"
    private const val KEY_HIDE_SMS = "hide_spam_sms"
    private const val KEY_LOG = "spam_log"
    private const val KEY_CALL_ENABLED = "call_guard_enabled"
    private const val KEY_SMS_ENABLED = "sms_guard_enabled"
    private const val KEY_LAST_CALL = "last_screened_call"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun digits(value: String): String = value.filter(Char::isDigit)

    fun blockedNumbers(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet()

    fun addBlocked(context: Context, number: String) {
        val value = digits(number)
        if (value.isEmpty()) return
        prefs(context).edit().apply {
            putStringSet(KEY_BLOCKED, blockedNumbers(context) + value)
        }.apply()
    }

    fun removeBlocked(context: Context, number: String) {
        prefs(context).edit().apply {
            putStringSet(KEY_BLOCKED, blockedNumbers(context) - number)
        }.apply()
    }

    fun clearBlocked(context: Context) {
        prefs(context).edit().remove(KEY_BLOCKED).apply()
    }

    /**
     * Match exact digits, or compare the last 10 digits so country codes and
     * trunk prefixes (for example +91 98765 43210 vs 098765 43210) still match.
     */
    fun isBlocked(context: Context, number: String?): Boolean {
        val incoming = digits(number.orEmpty())
        if (incoming.length < 4) return false
        return blockedNumbers(context).any { entry ->
            val saved = digits(entry)
            if (saved.length < 4) return@any false
            if (incoming == saved) return@any true
            val compared = minOf(incoming.length, saved.length, 10)
            compared >= 7 && incoming.takeLast(compared) == saved.takeLast(compared)
        }
    }

    fun recordScreenedCall(context: Context, label: String, blocked: Boolean) {
        val shown = label.ifBlank { "Hidden / unknown" }
        prefs(context).edit()
            .putString(KEY_LAST_CALL, "$shown · ${if (blocked) "BLOCKED" else "ALLOWED"}")
            .apply()
    }

    fun lastScreenedCall(context: Context): String? =
        prefs(context).getString(KEY_LAST_CALL, null)

    fun callGuardEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALL_ENABLED, true)

    fun setCallGuardEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALL_ENABLED, enabled).apply()
    }

    fun smsGuardEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SMS_ENABLED, true)

    fun setSmsGuardEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SMS_ENABLED, enabled).apply()
    }

    fun keywordFilter(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEYWORDS, true)

    fun setKeywordFilter(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_KEYWORDS, enabled).apply()
    }

    fun blockUnknown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UNKNOWN, false)

    fun setBlockUnknown(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_UNKNOWN, enabled).apply()
    }

    fun operatorSpamEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OPERATOR_SPAM, true)

    fun setOperatorSpamEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OPERATOR_SPAM, enabled).apply()
    }

    /** Hide spam SMS notifications; the message stays in the inbox. */
    fun hideSpamSms(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_SMS, true)

    fun setHideSpamSms(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_SMS, enabled).apply()
    }

    private val OPERATOR_MARKERS = listOf(
        "HUTCH", "HUTCHISON", "MOBITEL", "SLT", "DIALOG", "AIRTEL",
    )

    fun isOperatorSender(sender: String): Boolean =
        OPERATOR_MARKERS.any { sender.contains(it, ignoreCase = true) }

    /** Strong promo/phishing phrases are worth two points. */
    private val STRONG_SPAM_PHRASES = listOf(
        "click here", "you have won", "you've won", "congratulation", "lottery",
        "cash prize", "free gift", "claim your", "you are selected",
        "you've been selected", "guaranteed", "act now", "act fast",
        "limited time", "exclusive offer", "last chance", "urgent action",
        "winner", "prize",
    )

    /** Weaker promo signals are worth one point. */
    private val SPAM_PHRASES = listOf(
        "verify your account", "account suspended", "work from home",
        "earn money fast", "promotion", "promo code", "special offer",
        "offer", "bonus", "cashback", "unlimited", "% off", "save up to",
        "special deal", "discount", "voucher", "top up", "topup",
        "recharge", "data bonus", "free data", "win a", "personal loan",
        "loan offer", "subscribe", "unsubscribe", "dial *", "missed call",
        "sim card", "kyc", "disconnected", "electricity bill", "tax refund",
        "parcel", "delivery", "reschedule", "track your", "customs fee",
    )

    private val SHORT_LINK_HOSTS = listOf(
        "bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly", "cutt.ly", "is.gd", "s.id",
    )

    private val URL_PATTERN = Regex("""https?://|www\.""", RegexOption.IGNORE_CASE)

    /**
     * Heuristic only. Strong phrases count double. Short codes, operator
     * senders, and unknown mobile numbers need two points; saved contact names
     * need four. OTP and balance messages intentionally score zero.
     */
    fun isSpamSms(context: Context, body: String, sender: String = ""): Boolean {
        if (!keywordFilter(context)) return false
        val lower = body.lowercase()
        var score = STRONG_SPAM_PHRASES.count { lower.contains(it) } * 2 +
            SPAM_PHRASES.count { lower.contains(it) }
        if (SHORT_LINK_HOSTS.any { lower.contains(it) }) score += 2
        else if (URL_PATTERN.containsMatchIn(lower)) score++

        val senderDigits = sender.filter(Char::isDigit)
        val shortCode = senderDigits.isNotEmpty() && senderDigits.length <= 6
        val operatorSender = isOperatorSender(sender)

        val threshold = when {
            operatorSender && operatorSpamEnabled(context) -> 2
            shortCode -> 2
            senderDigits.isNotEmpty() -> 2
            else -> 4
        }
        return score >= threshold
    }

    fun log(context: Context): List<String> =
        prefs(context).getString(KEY_LOG, "").orEmpty()
            .lines()
            .filter { it.isNotBlank() }

    fun addLog(context: Context, entry: String) {
        val updated = (listOf(entry) + log(context)).take(40)
        prefs(context).edit().putString(KEY_LOG, updated.joinToString("\n")).apply()
    }

    fun clearLog(context: Context) {
        prefs(context).edit().remove(KEY_LOG).apply()
    }
}
