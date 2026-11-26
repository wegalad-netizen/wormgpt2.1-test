package com.wegalad.wormgpt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SentinalRulesTest {

    @Test
    fun normalizesBlocklistPatterns() {
        assertEquals("7414*", SentinalRules.normalizePattern(" 7414* "))
        assertEquals("*555", SentinalRules.normalizePattern(" *555 "))
        assertEquals("", SentinalRules.normalizePattern("abc"))
    }

    @Test
    fun matchesWildcardPatterns() {
        assertTrue(SentinalRules.wildcardMatch("7414*", "74141234"))
        assertTrue(SentinalRules.wildcardMatch("*555", "94771234555"))
        assertTrue(SentinalRules.wildcardMatch("*9*", "1239456"))
        assertFalse(SentinalRules.wildcardMatch("7414", "74141234"))
    }

    @Test
    fun matchesLastTenDigits() {
        assertTrue(SentinalRules.isBlockedEntry("98771234567", "88771234567"))
        assertTrue(SentinalRules.isBlockedEntry("0771234567", "940771234567"))
        assertFalse(SentinalRules.isBlockedEntry("12345", "94771234567"))
    }

    @Test
    fun detectsApiKeyPrefixes() {
        assertTrue(SentinalRules.looksLikeApiKey("AIzaSyD1234567890abcdefghijklmnop"))
        assertTrue(SentinalRules.looksLikeApiKey("oc_sk_a77f74289bee_d95I4AVKFkbHDSxyvF35gMrFGp4V5umG"))
        assertFalse(SentinalRules.looksLikeApiKey("gemini-2.5-flash"))
    }

    @Test
    fun validatesKeyPrefixWarnings() {
        assertNull(SentinalRules.apiKeyPrefixWarning("AIzaSyD1234567890abcdefghij", "gemini"))
        assertTrue(
            SentinalRules.apiKeyPrefixWarning("gsk_abc", "gemini")!!
                .contains("gemini"),
        )
        assertNull(SentinalRules.apiKeyPrefixWarning("anything", "opencode"))
    }

    @Test
    fun parsesBuildNumber() {
        assertEquals(105, SentinalRules.parseBuildNumber("Build number: 105\n\nWhat's new: test"))
        assertNull(SentinalRules.parseBuildNumber("no build here"))
    }

    @Test
    fun sanitizesLogs() {
        val input = "SMS 0771234567 user@example.com code 1234 token abcdefghijklmnopqrstuvwxyz1234567890"
        val output = SentinalRules.sanitizeLogEntry(input)
        assertTrue(output.contains("[PHONE REDACTED]"))
        assertTrue(output.contains("[EMAIL REDACTED]"))
        assertTrue(output.contains("[CODE REDACTED]"))
        assertTrue(output.contains("[TOKEN REDACTED]"))
    }
}
