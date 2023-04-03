package com.tangem.feature.onboarding.domain

/**
[REDACTED_AUTHOR]
 */
interface SeedPhraseChecker {
    suspend fun isSeedPhraseMatch(seedPhrase: List<String>): Boolean
    suspend fun isPhraseMatch(phrase: String, source: List<String>): Boolean
}

class DummySeedPhraseChecker : SeedPhraseChecker {

    override suspend fun isSeedPhraseMatch(seedPhrase: List<String>): Boolean = false

    override suspend fun isPhraseMatch(phrase: String, source: List<String>): Boolean = source.contains(phrase)
}