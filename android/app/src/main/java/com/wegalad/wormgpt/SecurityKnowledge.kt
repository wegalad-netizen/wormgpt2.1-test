package com.wegalad.wormgpt

/** Small offline knowledge base used to enrich AI prompts with defensive frameworks. */
object SecurityKnowledge {

    data class Entry(val title: String, val summary: String, val points: List<String>)

    val entries: List<Entry> = listOf(
        Entry(
            "OWASP MASVS (Mobile)",
            "Mobile Application Security Verification Standard categories for defensive mobile apps.",
            listOf(
                "MASVS-STORAGE: protect sensitive data at rest",
                "MASVS-CRYPTO: use platform crypto correctly",
                "MASVS-AUTH: secure authentication and session management",
                "MASVS-NETWORK: TLS, certificate validation, pinning",
                "MASVS-PLATFORM: permissions, intents, WebView, IPC",
                "MASVS-CODE: input validation, memory and code quality",
                "MASVS-RESILIENCE: anti-tamper, anti-debugging, obfuscation",
                "MASVS-INTERACTION: UI redressing, phishing, sensitive input",
            ),
        ),
        Entry(
            "OWASP Top 10 (Web)",
            "Ten critical web application risk categories, useful for review prompts.",
            listOf(
                "A01 Broken Access Control",
                "A02 Cryptographic Failures",
                "A03 Injection",
                "A04 Insecure Design",
                "A05 Security Misconfiguration",
                "A06 Vulnerable and Outdated Components",
                "A07 Identification and Authentication Failures",
                "A08 Software and Data Integrity Failures",
                "A09 Security Logging and Monitoring Failures",
                "A10 Server-Side Request Forgery",
            ),
        ),
        Entry(
            "MITRE ATT&CK Tactics",
            "Fourteen adversary tactics across the attack lifecycle.",
            listOf(
                "Reconnaissance, Resource Development, Initial Access",
                "Execution, Persistence, Privilege Escalation",
                "Defense Evasion, Credential Access, Discovery",
                "Lateral Movement, Collection, Command and Control",
                "Exfiltration, Impact",
            ),
        ),
        Entry(
            "NIST CSF 2.0",
            "Six functions for organizing cybersecurity risk management.",
            listOf(
                "GOVERN: policies, roles, risk appetite",
                "IDENTIFY: assets, risks, supply chain",
                "PROTECT: access control, data security, awareness",
                "DETECT: anomalies, continuous monitoring",
                "RESPOND: triage, containment, communication",
                "RECOVER: restoration and improvement",
            ),
        ),
        Entry(
            "STRIDE Threat Model",
            "Six categories for systematic threat discovery.",
            listOf(
                "Spoofing identity",
                "Tampering with data",
                "Repudiation of actions",
                "Information disclosure",
                "Denial of service",
                "Elevation of privilege",
            ),
        ),
        Entry(
            "Cyber Kill Chain",
            "Seven phases used to describe an intrusion.",
            listOf(
                "Reconnaissance, Weaponization, Delivery",
                "Exploitation, Installation",
                "Command and Control, Actions on Objectives",
            ),
        ),
        Entry(
            "Incident Response Lifecycle",
            "Six-phase defensive response workflow.",
            listOf(
                "Preparation and playbooks",
                "Identification and scoping",
                "Containment (short and long term)",
                "Eradication of root cause",
                "Recovery and validation",
                "Lessons learned and hardening",
            ),
        ),
        Entry(
            "Secure Coding Checklist",
            "Practical defensive coding fundamentals.",
            listOf(
                "Validate and canonicalize all input",
                "Apply context-aware output encoding",
                "Enforce authentication and authorization everywhere",
                "Never hardcode secrets; use keystore/secret manager",
                "Apply least privilege to components and APIs",
                "Use vetted crypto with secure randomness",
                "Fail safely; avoid leaking internals in errors",
                "Log security events without sensitive data",
                "Pin and update dependencies; monitor advisories",
            ),
        ),
        Entry(
            "Privacy by Design",
            "Principles for minimizing privacy risk.",
            listOf(
                "Data minimization and purpose limitation",
                "Clear retention and deletion schedules",
                "Encryption in transit and at rest",
                "Role-based access and audit trails",
                "User consent and transparent notices",
            ),
        ),
        Entry(
            "Sinhala Security Terms",
            "Sinhala vocabulary for bilingual security reporting.",
            listOf(
                "Confidentiality — රහස්‍යතාව",
                "Integrity — අඛණ්ඩතාව",
                "Availability — ලබාගත හැකියාව",
                "Threat — තර්ජනය",
                "Risk — අවදානම",
                "Vulnerability — දුර්වලතාව",
                "Incident — සිද්ධිය",
                "Authentication — සත්‍යාපනය",
            ),
        ),
    )

    fun format(entry: Entry): String = buildString {
        append(entry.title).append('\n')
        append(entry.summary).append("\n\n")
        entry.points.forEach { append("- ").append(it).append('\n') }
    }
}
