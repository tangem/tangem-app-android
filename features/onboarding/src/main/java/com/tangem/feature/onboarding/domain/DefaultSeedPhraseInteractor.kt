package com.tangem.feature.onboarding.domain

import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.MnemonicErrorResult
import com.tangem.feature.onboarding.data.MnemonicRepository

/**
* [REDACTED_AUTHOR]
 */
internal class DefaultSeedPhraseInteractor constructor(
    private val repository: MnemonicRepository,
) : SeedPhraseInteractor {

    private var currentMnemonic: Mnemonic? = null

    override suspend fun generateMnemonic(): Result<Mnemonic> {
        return runCatching {
            repository.generateDefaultMnemonic().apply {
                currentMnemonic = this
            }
        }
    }

    override suspend fun getMnemonicComponents(): Result<List<String>> {
        return runCatching {
            val mnemonic = currentMnemonic ?: repository.generateDefaultMnemonic().apply {
                currentMnemonic = this
            }
            mnemonic.mnemonicComponents
        }
    }

    override suspend fun isWordMatch(word: String): Boolean {
        return repository.getWordsDictionary().contains(word)
    }

    override suspend fun validateMnemonicString(text: String): Result<List<String>> {
        val inputWords = text.split("\\s+".toRegex()).toSet()
        val wordsDictionary = repository.getWordsDictionary()

        val invalidWords = inputWords
            .filter { it.isNotEmpty() && !wordsDictionary.contains(it) }
            .toSet()

        return if (invalidWords.isNotEmpty()) {
            Result.failure(SeedPhraseError.InvalidWords(invalidWords))
        } else {
            try {
                val mnemonic = repository.createMnemonic(text)
                Result.success(mnemonic.mnemonicComponents)
            } catch (ex: TangemSdkError.MnemonicException) {
                val error = ex.mnemonicResult.mapToError()
                Result.failure(error)
            }
        }
    }

    override suspend fun getSuggestions(
        text: String,
        hasSelection: Boolean,
        cursorPosition: Int,
    ): List<String> {
// [REDACTED_TODO_COMMENT]
        return emptyList()
    }

    override suspend fun insertSuggestionWord(
        text: String,
        suggestion: String,
        cursorPosition: Int,
    ): InsertSuggestionResult {
// [REDACTED_TODO_COMMENT]
        return InsertSuggestionResult("", 0)
    }
}

private fun MnemonicErrorResult.mapToError(): SeedPhraseError = when (this) {
    MnemonicErrorResult.InvalidWordCount -> SeedPhraseError.InvalidEntropyLength
    MnemonicErrorResult.InvalidEntropyLength -> SeedPhraseError.InvalidEntropyLength
    MnemonicErrorResult.InvalidWordsFile -> SeedPhraseError.InvalidWordsFile
    MnemonicErrorResult.InvalidChecksum -> SeedPhraseError.InvalidChecksum
    MnemonicErrorResult.MnenmonicCreationFailed -> SeedPhraseError.MnenmonicCreationFailed
    MnemonicErrorResult.NormalizationFailed -> SeedPhraseError.NormalizationFailed
    MnemonicErrorResult.UnsupportedLanguage -> SeedPhraseError.UnsupportedLanguage
    is MnemonicErrorResult.InvalidWords -> SeedPhraseError.InvalidWords(this.words)
}
