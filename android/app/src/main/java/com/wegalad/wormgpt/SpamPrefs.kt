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
        prefs(context).getBoolean(KEY_OPERATOR_SPAM, false)

    fun setOperatorSpamEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OPERATOR_SPAM, enabled).apply()
    }

    private val SPAM_PHRASES = listOf(
        "click here", "limited time", "act now", "act fast", "you have won",
        "you've won", "congratulations", "lottery", "free gift", "claim your",
        "verify your account", "account suspended", "bank account", "one time password",
        "urgent action", "last chance", "exclusive offer", "guaranteed", "cash prize",
        "work from home", "earn money fast", "confirm your password",
    )

    private val SHORT_LINK_HOSTS = listOf(
        "bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly", "cutt.ly", "is.gd", "s.id",
    )

    /** Heuristic only: two or more signals means a warning, not silent deletion. */
    fun isSpamSms(context: Context, body: String): Boolean {
        if (!keywordFilter(context)) return false
        val lower = body.lowercase()
        var score = SPAM_PHRASES.count { lower.contains(it) }
        if (SHORT_LINK_HOSTS.any { lower.contains(it) }) score++
        return score >= 2
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
