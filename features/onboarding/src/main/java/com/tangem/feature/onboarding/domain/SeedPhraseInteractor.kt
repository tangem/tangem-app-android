package com.tangem.feature.onboarding.domain

import com.tangem.crypto.bip39.Mnemonic

/**
* [REDACTED_AUTHOR]
 */
interface SeedPhraseInteractor {
    suspend fun generateMnemonic(): Result<Mnemonic>
    suspend fun getMnemonicComponents(): Result<List<String>>
    suspend fun isWordMatch(word: String): Boolean
    suspend fun validateMnemonicString(text: String): Result<List<String>>
    suspend fun getSuggestions(text: String, hasSelection: Boolean, cursorPosition: Int): List<String>
    suspend fun insertSuggestionWord(text: String, suggestion: String, cursorPosition: Int): InsertSuggestionResult

    companion object {
        const val MNEMONIC_DELIMITER = " "
    }
}

data class InsertSuggestionResult(
    val text: String,
    val cursorPosition: Int,
)
