package com.tangem.feature.onboarding.domain

/**
 * Created by Anton Zhilenkov on 16.03.2023.
 */
interface SeedPhraseChecker {
    suspend fun isSeedPhraseMatch(seedPhrase: List<String>): Boolean
    suspend fun isPhraseMatch(phrase: String, source: List<String>): Boolean
}

class DummySeedPhraseChecker : SeedPhraseChecker {

    override suspend fun isSeedPhraseMatch(seedPhrase: List<String>): Boolean = false

    override suspend fun isPhraseMatch(phrase: String, source: List<String>): Boolean = source.contains(phrase)
}
