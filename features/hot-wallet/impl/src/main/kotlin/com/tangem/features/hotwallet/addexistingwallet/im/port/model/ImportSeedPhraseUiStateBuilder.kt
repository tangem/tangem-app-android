package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.core.TangemSdkError
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.MnemonicErrorResult
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.entity.AddExistingWalletImportUM
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class ImportSeedPhraseUiStateBuilder(
    private val modelScope: CoroutineScope,
    private val mnemonicRepository: MnemonicRepository,
    private val readyToImport: (Boolean) -> Unit,
    private val updateUiState: ((AddExistingWalletImportUM) -> AddExistingWalletImportUM) -> Unit,
    private val importWallet: (mnemonic: Mnemonic, passphrase: String?) -> Unit,
    private val onPassphraseInfoClick: () -> Unit,
    private val onImportClick: () -> Unit,
) {
    private val wordsCheckJobHolder = JobHolder()
    private var importedMnemonic: Mnemonic? = null
    private var passphrase: String? = null

    fun getState(): AddExistingWalletImportUM {
        return AddExistingWalletImportUM(
            words = TextFieldValue(""),
            passPhrase = TextFieldValue(""),
            wordsErrorText = null,
            invalidWords = persistentListOf(),
            isImportWalletEnabled = false,
            isImportWalletInProgress = false,
            suggestionsList = persistentListOf(),
            wordsChange = { wordsField ->
                launchInterceptWords(wordsField = wordsField)
                suggestNextWord(wordsField)
                updateUiState { state ->
                    state.copy(words = wordsField)
                }
            },
            passPhraseChange = { passPhraseValue ->
                passphrase = passPhraseValue.text
                updateUiState { state -> state.copy(passPhrase = passPhraseValue) }
            },
            onPassphraseInfoClick = onPassphraseInfoClick,
            importWalletClick = ::onCreateWallet,
            onSuggestionClick = { word -> addSuggestedWord(word) },
            isReadyToImport = false,
        )
    }

    private fun onCreateWallet() {
        onImportClick()
        val mnemonic = importedMnemonic ?: return
        val passphrase = passphrase?.takeIf { it.isNotEmpty() }
        importWallet(mnemonic, passphrase)
    }

    private fun addSuggestedWord(word: String) {
        updateUiState { st ->
            val text = st.words.text
            val wordsFromText = text.split(" ").filter { it.isNotBlank() }.map { it.trim() }
            val newWords = wordsFromText.dropLast(1) + word
            val newWordsText = newWords.joinToString(" ").plus(" ")
            val newWordsState = TextFieldValue(
                text = newWordsText,
                selection = TextRange(newWordsText.length),
            )
            st.copy(
                words = newWordsState,
            ).also {
                launchInterceptWords(wordsField = newWordsState)
                suggestNextWord(newWordsState)
            }
        }
    }

    private fun suggestNextWord(wordsField: TextFieldValue) {
        if (wordsField.text.endsWith(" ")) {
            updateUiState { it.copy(suggestionsList = emptyList<String>().toImmutableList()) }
            return
        }

        val text = wordsField.text
        val wordsFromText = text.split(" ").filter { it.isNotBlank() }.map { it.trim() }
        val lastWord = wordsFromText.lastOrNull() ?: return
        if (lastWord.length < MINIMUM_WORD_LENGTH) return

        if (mnemonicRepository.words.contains(lastWord) && mnemonicRepository.words.none { it.startsWith(lastWord) }) {
            updateUiState { it.copy(suggestionsList = emptyList<String>().toImmutableList()) }
            return
        }

        updateUiState { st ->
            st.copy(
                suggestionsList = mnemonicRepository.words
                    .filter { it.startsWith(lastWord) && it != lastWord }
                    .toImmutableList(),
            )
        }
    }

    private fun launchInterceptWords(wordsField: TextFieldValue) {
        modelScope.launch {
            delay(timeMillis = WORDS_INTERCEPT_DELAY_MS)
            interceptWords(wordsField)
        }.saveIn(wordsCheckJobHolder)
    }

    private fun interceptWords(wordsField: TextFieldValue) {
        readyToImport(false)

        updateUiState { state ->
            state.copy(
                isImportWalletEnabled = false,
                wordsErrorText = null,
            )
        }

        importedMnemonic = null

        val text = wordsField.text
        val wordsFromText = text.split(" ").filter { it.isNotBlank() }.map { it.trim() }
        val invalidWords = wordsFromText.filterNot { it in mnemonicRepository.words }
        if (invalidWords.isNotEmpty()) {
            updateUiState { state ->
                state.copy(
                    invalidWords = invalidWords.toImmutableList(),
                    wordsErrorText = resourceReference(R.string.onboarding_seed_mnemonic_wrong_words),
                    isImportWalletEnabled = false,
                )
            }
            return
        }

        try {
            val mnemonic = mnemonicRepository.generateMnemonic(text)
            importedMnemonic = mnemonic
            updateUiState { state ->
                state.copy(
                    invalidWords = emptyList<String>().toImmutableList(),
                    wordsErrorText = null,
                    isImportWalletEnabled = true,
                )
            }
            readyToImport(true)
        } catch (ex: TangemSdkError.MnemonicException) {
            readyToImport(false)
            val error = ex.mnemonicResult
            if (error is MnemonicErrorResult.InvalidChecksum) {
                updateUiState { state ->
                    state.copy(
                        wordsErrorText = resourceReference(R.string.onboarding_seed_mnemonic_invalid_checksum),
                        isImportWalletEnabled = false,
                    )
                }
            } else {
                updateUiState { state ->
                    state.copy(
                        isImportWalletEnabled = false,
                        wordsErrorText = null,
                    )
                }
            }
        }
    }

    companion object {
        private const val MINIMUM_WORD_LENGTH = 2
        private const val WORDS_INTERCEPT_DELAY_MS = 500L
    }
}