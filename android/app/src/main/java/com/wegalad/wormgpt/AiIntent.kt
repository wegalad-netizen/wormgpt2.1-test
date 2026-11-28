package com.wegalad.wormgpt

/** Lightweight local intent detection used by SMART mode to enrich prompts. */
object AiIntent {

    private val codePattern = Regex(
        """(?i)\b(code|function|script|bug|error|exception|python|kotlin|java|javascript|bash|regex|compile|snippet|stack trace|`{3})\b""",
    )
    private val linkPattern = Regex(
        """(?i)\b(url|link|website|domain|http://|https://|www\.)\b""",
    )
    private val phishingPattern = Regex(
        """(?i)\b(phish|suspicious message|sms|email|scam|fraud|lottery|click here|verify your account)\b""",
    )
    private val threatPattern = Regex(
        """(?i)\b(threat|attack|malware|ransomware|exploit|vulnerability|attacker|actor|ioc|indicator|breach)\b""",
    )
    private val incidentPattern = Regex(
        """(?i)\b(incident|breach|compromised|containment|eradication|forensic|response plan|sandbox)\b""",
    )

    fun detect(text: String): String {
        val lower = text.lowercase()
        return when {
            containsSinhala(lower) -> "SINHALA"
            incidentPattern.containsMatchIn(lower) -> "INCIDENT"
            phishingPattern.containsMatchIn(lower) -> "PHISHING"
            linkPattern.containsMatchIn(lower) -> "LINK"
            codePattern.containsMatchIn(lower) -> "CODE"
            threatPattern.containsMatchIn(lower) -> "THREAT"
            else -> "GENERAL"
        }
    }

    fun instructionFor(intent: String): String = when (intent) {
        "CODE" ->
            "Review the code as a defensive security engineer. Identify concrete flaws, severity, and corrected snippets. Never execute code."
        "LINK" ->
            "Assess the URL for phishing and safety signals. Explain the evidence, likely purpose, and safer alternatives."
        "PHISHING" ->
            "Triage this message for phishing indicators. List red flags, likely goal, and safe next steps without clicking anything."
        "THREAT" ->
            "Provide a defensive threat brief: indicators, TTP context, impact, likelihood, mitigations, and confidence."
        "INCIDENT" ->
            "Give a practical incident-response checklist: scope, containment, eradication, recovery, evidence, and lessons learned."
        "SINHALA" ->
            "Respond in clear Sinhala (සිංහල) with practical defensive guidance. Keep technical terms understandable."
        else ->
            "Give a concise, practical, security-focused answer with assumptions and confidence."
    }

    fun smartInstruction(text: String): String = instructionFor(detect(text))

    private fun containsSinhala(text: String): Boolean =
        text.any { it.code in 0x0D80..0x0DFF }
}
