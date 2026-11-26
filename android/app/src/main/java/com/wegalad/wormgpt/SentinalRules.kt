package com.wegalad.wormgpt

/** Pure, dependency-free rules shared by the app and unit tests. */
object SentinalRules {

    fun digits(value: String): String = value.filter { it.isDigit() }

    fun normalizePattern(raw: String): String =
        raw.trim().lowercase().filter { it.isDigit() || it == '*' }

    fun wildcardMatch(entry: String, incoming: String): Boolean {
        val starts = entry.startsWith("*")
        val ends = entry.endsWith("*")
        val body = entry.removePrefix("*").removeSuffix("*").trim()
        if (body.isEmpty()) return true
        return when {
            starts && ends -> incoming.contains(body)
            starts -> incoming.endsWith(body)
            ends -> incoming.startsWith(body) || incoming.endsWith(body)
            else -> false
        }
    }

    fun isBlockedEntry(incoming: String, entry: String): Boolean {
        if (entry.contains('*')) return wildcardMatch(entry, incoming)
        val saved = digits(entry)
        if (saved.length < 4) return false
        if (incoming == saved) return true
        val compared = minOf(incoming.length, saved.length, 10)
        return compared >= 7 && incoming.takeLast(compared) == saved.takeLast(compared)
    }

    fun looksLikeApiKey(value: String): Boolean {
        val candidate = value.trim()
        return candidate.length >= 24 && (
            candidate.startsWith("sk-", ignoreCase = true) ||
                candidate.startsWith("gsk_", ignoreCase = true) ||
                candidate.startsWith("oc_sk_", ignoreCase = true) ||
                candidate.startsWith("oc_", ignoreCase = true) ||
                candidate.startsWith("AIza", ignoreCase = true) ||
                (candidate.length >= 32 && !candidate.contains('/') &&
                    !candidate.startsWith("gemini", ignoreCase = true))
            )
    }

    fun apiKeyPrefixWarning(key: String, provider: String): String? {
        val expected = when (provider) {
            "gemini" -> "AIza"
            "openai", "together" -> "sk-"
            "openrouter" -> "sk-or-"
            "anthropic" -> "sk-ant-"
            "groq" -> "gsk_"
            else -> return null
        }
        return if (key.startsWith(expected, ignoreCase = true)) {
            null
        } else {
            "This does not look like a $provider key (expected $expected…)."
        }
    }

    fun parseBuildNumber(body: String): Int? =
        Regex("Build number:\\s*(\\d+)").find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    fun sanitizeLogEntry(entry: String): String {
        var value = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")
            .replace(entry, "[EMAIL REDACTED]")
        value = Regex("""(?:\+?94|0)?7\d{8}""").replace(value, "[PHONE REDACTED]")
        value = Regex("""\b\d{4,8}\b""").replace(value, "[CODE REDACTED]")
        return Regex("""[A-Za-z0-9+/=]{32,}""").replace(value, "[TOKEN REDACTED]")
    }
}
