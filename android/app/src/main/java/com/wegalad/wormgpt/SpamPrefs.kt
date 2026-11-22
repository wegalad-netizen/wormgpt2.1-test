package com.wegalad.wormgpt

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared storage for Sentinal Security. The activity, call screener and SMS receiver
 * all read the same settings so the UI controls actually affect blocking.
 */
data class QuarantineItem(
    val sender: String,
    val preview: String,
    val score: Int,
    val verdict: String,
    val created: Long,
    val signals: List<String>,
)

object SpamPrefs {
    private const val PREFS = "sentinal_spam_prefs"
    private const val KEY_BLOCKED = "blocked_numbers"
    private const val KEY_KEYWORDS = "keyword_filter"
    private const val KEY_UNKNOWN = "block_unknown"
    private const val KEY_OPERATOR_SPAM = "operator_spam"
    private const val KEY_HIDE_SMS = "hide_spam_sms"
    private const val KEY_HIDE_OTP_SMS = "hide_otp_sms"
    private const val KEY_LOG = "spam_log"
    private const val KEY_CALL_ENABLED = "call_guard_enabled"
    private const val KEY_SMS_ENABLED = "sms_guard_enabled"
    private const val KEY_LAST_CALL = "last_screened_call"
    private const val KEY_QUARANTINE = "quarantine_entries"
    private const val MAX_QUARANTINE = 80
    private const val LOG_LIMIT = 200

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun digits(value: String): String = value.filter(Char::isDigit)

    fun blockedNumbers(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet()

    /**
     * Add a blocked number or wildcard pattern, e.g. "5550100", "+1 555 0100"
     * or "7414*". Patterns are stored as digits plus '*' characters.
     */
    fun addBlocked(context: Context, pattern: String) {
        val value = normalizePattern(pattern)
        if (value.isEmpty() || value.count { it.isDigit() } == 0 || value.length < 3) return
        if (blockedNumbers(context).contains(value)) return
        prefs(context).edit().apply {
            putStringSet(KEY_BLOCKED, blockedNumbers(context) + value)
        }.apply()
    }

    /** Import entries (one per line); returns the number of new entries added. */
    fun importBlocklist(context: Context, text: String): Int {
        val existing = blockedNumbers(context).toMutableSet()
        var added = 0
        text.lines().forEach { raw ->
            val value = normalizePattern(raw)
            if (value.isEmpty() || value.count { it.isDigit() } == 0 || value.length < 3) {
                return@forEach
            }
            if (value in existing) return@forEach
            existing += value
            added += 1
        }
        if (added > 0) prefs(context).edit().putStringSet(KEY_BLOCKED, existing).apply()
        return added
    }

    /** Export the blocklist as one entry per line. */
    fun exportBlocklist(context: Context): String =
        blockedNumbers(context).sorted().joinToString("\n")

    private fun normalizePattern(raw: String): String =
        raw.trim().lowercase().filter { it.isDigit() || it == '*' }

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
     * Entries containing a '*' are wildcard patterns, e.g. "7414*".
     */
    fun isBlocked(context: Context, number: String?): Boolean {
        val incoming = digits(number.orEmpty())
        if (incoming.length < 4) return false
        return blockedNumbers(context).any { entry -> isBlockedEntry(incoming, entry) }
    }

    private fun isBlockedEntry(incoming: String, entry: String): Boolean {
        if (entry.contains('*')) return wildcardMatch(entry, incoming)
        val saved = digits(entry)
        if (saved.length < 4) return false
        if (incoming == saved) return true
        val compared = minOf(incoming.length, saved.length, 10)
        return compared >= 7 && incoming.takeLast(compared) == saved.takeLast(compared)
    }

    /** Leading/trailing '*' wildcard: "7414*", "*555", "*9*". */
    private fun wildcardMatch(entry: String, incoming: String): Boolean {
        val starStart = entry.startsWith("*")
        val starEnd = entry.endsWith("*")
        val body = entry.removePrefix("*").removeSuffix("*").trim()
        if (body.isEmpty()) return true
        return when {
            starStart && starEnd -> incoming.contains(body)
            starStart -> incoming.endsWith(body)
            starEnd -> incoming.startsWith(body) || incoming.endsWith(body)
            else -> false
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

    /** Hide OTP-like SMS notifications only; SMS messages remain in the inbox. */
    fun hideOtpSms(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_OTP_SMS, false)

    fun setHideOtpSms(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_OTP_SMS, enabled).apply()
    }

    fun isOneTimeCodeMessage(body: String): Boolean {
        val hasCodeContext = OTP_CONTEXT.containsMatchIn(body)
        val hasNumericCode = OTP_NUMERIC_CODE.containsMatchIn(body)
        return hasCodeContext && hasNumericCode
    }

    private val OTP_CONTEXT = Regex(
        """\b(?:otp|one[- ]time (?:password|code|pin)|verification code|verify(?:ication)? code|security code|login code|authentication code|passcode|pin)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val OTP_NUMERIC_CODE = Regex("\\b\\d{4,8}\\b")

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
        "suspicious sign-in", "suspicious login", "unusual sign-in attempt",
        "sign-in attempt detected", "your account was accessed",
        "your account has been accessed", "someone accessed your account",
        "your card will be blocked", "your card is being blocked",
        "verify your card", "verify your account now", "verify now",
        "click the link to verify", "your money is at risk",
        "unusual activity detected", "unauthorized transaction",
        "final warning", "temporary password", "your account will expire",
        "review your subscription", "update your details",
        "your package is lost", "package is lost", "order was not placed",
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
        "verify your email", "email verification", "confirm your email",
        "account not verified", "please verify",
        "update your payment", "update your payment details",
        "invoice attached", "unpaid invoice", "overdue invoice",
        "package delayed", "your package is delayed",
        "unsubscribe from this message", "please unsubscribe",
        "urgent", "don't miss out", "limited quantity", "today only",
        "expires soon", "earn money", "investment opportunity", "invest now",
        "gift card", "unclaimed funds", "unclaimed refund", "apple id",
        "your subscription renewal", "renew your subscription", "free trial",
        "someone you know", "your subscription ends soon",
    )

    private val SHORT_LINK_HOSTS = listOf(
        "bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly", "cutt.ly", "is.gd", "s.id",
        "bit.do", "tiny.cc", "j.gs", "n.hid", "v.ht", "adf.ly", "t.ly", "u.to",
        "rebrand.ly", "clck.ru",
    )

    private val URLS_PATTERN = Regex("""https?://[^\s<>"']+|www\.[^\s<>"']+""", RegexOption.IGNORE_CASE)

    private val SUSPICIOUS_URL_VERBS = listOf(
        "click", "verify", "claim", "login", "signin", "sign-in", "sign in",
        "update", "redeem", "confirm", "secure", "recover", "reset", "approve",
        "pay now", "send money", "transfer", "buy now", "order now", "open now",
    )

    private val EMAIL_SENDER = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")

    /**
     * Heuristic only. Strong phrases count double. Short codes, operator
     * senders, and unknown mobile numbers need two points; saved contact names
     * need four. OTP and balance messages intentionally score zero.
     */
    fun isSpamSms(context: Context, body: String, sender: String = ""): Boolean {
        if (!keywordFilter(context)) return false
        return spamScore(context, body, sender) >= spamThreshold(context, sender)
    }

    /** Weighted risk score from phrases, suspicious links, and sender signals. */
    fun spamScore(context: Context, body: String, sender: String = ""): Int {
        if (!keywordFilter(context)) return 0
        val lower = body.lowercase()
        val urls = URLS_PATTERN.findAll(lower).map { it.value }.toList()
        var score = STRONG_SPAM_PHRASES.count { lower.contains(it) } * 2 +
            SPAM_PHRASES.count { lower.contains(it) }
        if (urls.isNotEmpty()) {
            if (urls.any { url -> SUSPICIOUS_URL_VERBS.any { url.contains(it) } }) {
                score += 3
            } else if (SHORT_LINK_HOSTS.any { lower.contains(it) }) {
                score += 2
            } else {
                score++
            }
        }
        if (isEmailSender(sender)) score += 2
        return score
    }

    /** Labels of the signals that fired, for triage and reports. */
    fun spamSignals(context: Context, body: String, sender: String = ""): List<String> {
        if (!keywordFilter(context)) return emptyList()
        val lower = body.lowercase()
        val signals = mutableListOf<String>()
        STRONG_SPAM_PHRASES.filter { lower.contains(it) }.forEach { signals += it }
        SPAM_PHRASES.filter { lower.contains(it) }.forEach { signals += it }
        if (isEmailSender(sender)) signals += "Email-style sender address"
        val urls = URLS_PATTERN.findAll(lower).map { it.value }.toList()
        if (urls.any { url -> SUSPICIOUS_URL_VERBS.any { url.contains(it) } }) {
            signals += "Suspicious link (click/verify/login style)"
        } else if (urls.isNotEmpty()) {
            signals += "Link in message"
        }
        return signals.distinct()
    }

    private fun spamThreshold(context: Context, sender: String): Int {
        val senderDigits = sender.filter(Char::isDigit)
        return when {
            isOperatorSender(sender) && operatorSpamEnabled(context) -> 2
            senderDigits.isNotEmpty() -> 2
            else -> 4
        }
    }

    private fun isEmailSender(sender: String): Boolean =
        EMAIL_SENDER.containsMatchIn(sender)

    fun log(context: Context): List<String> =
        prefs(context).getString(KEY_LOG, "").orEmpty()
            .lines()
            .filter { it.isNotBlank() }

    fun addLog(context: Context, entry: String) {
        val line = "${timestampNow()} · $entry"
        val updated = (listOf(line) + log(context)).take(LOG_LIMIT)
        prefs(context).edit().putString(KEY_LOG, updated.joinToString("\n")).apply()
    }

    private fun timestampNow(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun clearLog(context: Context) {
        prefs(context).edit().remove(KEY_LOG).apply()
    }

    // ---- Quarantine / triage ----

    fun quarantine(context: Context): List<QuarantineItem> =
        parseQuarantine(prefs(context).getString(KEY_QUARANTINE, "") ?: "")

    fun addQuarantine(context: Context, item: QuarantineItem) {
        val updated = (listOf(item) + quarantine(context)).take(MAX_QUARANTINE)
        prefs(context).edit().putString(KEY_QUARANTINE, updated.toQuarantineJson()).apply()
    }

    fun removeQuarantine(context: Context, created: Long) {
        val updated = quarantine(context).filterNot { it.created == created }
        prefs(context).edit().putString(KEY_QUARANTINE, updated.toQuarantineJson()).apply()
    }

    fun clearQuarantine(context: Context) {
        prefs(context).edit().remove(KEY_QUARANTINE).apply()
    }

    fun quarantineCount(context: Context): Int = quarantine(context).size

    fun formatTimestamp(millis: Long): String =
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))

    // ---- Reports & history ----

    /** Full stats + quarantine detail + recent alert history as text. */
    fun generateReport(context: Context): String {
        val sb = StringBuilder()
        sb.append("SENTINAL SECURITY REPORT\n")
        sb.append("Generated: ")
            .append(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
            .append("\n")
        sb.append("\nBlocked numbers: ").append(blockedNumbers(context).size)
        sb.append("\nQuarantined messages: ").append(quarantineCount(context))
        sb.append("\nAlerts logged: ").append(log(context).size)
        sb.append("\nLast screened call: ").append(lastScreenedCall(context) ?: "none yet")
        sb.append("\nOperator spam auto-detect: ")
            .append(if (operatorSpamEnabled(context)) "On" else "Off")
        sb.append("\n\nQuarantine detail:\n")
        val items = quarantine(context)
        if (items.isEmpty()) sb.append("- (none)\n")
        items.take(25).forEach { q ->
            sb.append("- ").append(q.sender)
                .append(" · RISK ").append(q.score).append("%")
                .append(" · ").append(formatTimestamp(q.created))
                .append(" · ").append(q.preview.ifBlank { "(no preview)" })
            if (q.signals.isNotEmpty()) {
                sb.append("\n  Signals: ").append(q.signals.joinToString(", ").take(120))
            }
            sb.append("\n")
        }
        sb.append("\nRecent alert history (newest first):\n")
        log(context).forEach { sb.append("- ").append(it).append("\n") }
        return sb.toString()
    }

    private fun List<QuarantineItem>.toQuarantineJson(): String {
        val array = JSONArray()
        forEach { item ->
            val obj = JSONObject()
            obj.put("sender", item.sender)
            obj.put("preview", item.preview)
            obj.put("score", item.score)
            obj.put("verdict", item.verdict)
            obj.put("created", item.created)
            obj.put("signals", item.signals.toJsonArray())
            array.put(obj)
        }
        return array.toString()
    }

    private fun List<String>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { array.put(it) }
        return array
    }

    private fun parseQuarantine(text: String): List<QuarantineItem> {
        if (text.isBlank()) return emptyList()
        return try {
            val json = JSONArray(text)
            (0 until json.length()).mapNotNull { i ->
                val obj = json.optJSONObject(i) ?: return@mapNotNull null
                QuarantineItem(
                    sender = obj.optString("sender", ""),
                    preview = obj.optString("preview", ""),
                    score = obj.optInt("score", 0),
                    verdict = obj.optString("verdict", ""),
                    created = obj.optLong("created", 0L),
                    signals = obj.optJSONArray("signals")?.let { arr ->
                        (0 until arr.length()).map { j -> arr.getString(j) }
                    } ?: emptyList(),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
