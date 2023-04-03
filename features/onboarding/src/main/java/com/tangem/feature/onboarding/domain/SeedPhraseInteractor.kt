package com.tangem.feature.onboarding.domain

import com.tangem.feature.onboarding.data.DummySeedPhraseRepository
import com.tangem.feature.onboarding.data.SeedPhraseRepository

/**
* [REDACTED_AUTHOR]
 */
interface SeedPhraseInteractor : SeedPhraseChecker {
    suspend fun generateSeedPhrase(): List<String>
}

class DummySeedPhraseInteractor(
    private val sdkSeedPhraseRepository: SeedPhraseRepository = DummySeedPhraseRepository(),
    private val seedPhraseChecker: SeedPhraseChecker = DummySeedPhraseChecker(),
) : SeedPhraseInteractor, SeedPhraseChecker by seedPhraseChecker {

    override suspend fun generateSeedPhrase(): List<String> {
        return sdkSeedPhraseRepository.getWordList().wordlist
    }
}
