package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.core.TangemSdkError
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.MnemonicErrorResult
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.MnemonicRepository
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ImportSeedPhraseUiStateBuilder(
    private val modelScope: CoroutineScope,
    private val mnemonicRepository: MnemonicRepository,
    private val updateUiState: ((MultiWalletSeedPhraseUM.Import) -> MultiWalletSeedPhraseUM.Import) -> Unit,
    private val importWallet: (mnemonic: Mnemonic, passphrase: String?) -> Unit,
) {
    private val wordsCheckJobHolder = JobHolder()
    private var importedMnemonic: Mnemonic? = null
    private var passphrase: String? = null

    fun getState(): MultiWalletSeedPhraseUM {
        return MultiWalletSeedPhraseUM.Import(
            wordsChange = {
                launchInterceptWords(wordsField = it)
                suggestNextWord(it)
                updateUiState { state ->
                    state.copy(words = it)
                }
            },
            passPhraseChange = {
                passphrase = it.text
                updateUiState { state -> state.copy(passPhrase = it) }
            },
            onPassphraseInfoClick = ::showInfoBS,
            createWalletClick = ::onCreateWallet,
            onSuggestionClick = { word -> addSuggestedWord(word) },
        )
    }

    private fun onCreateWallet() {
        val mnemonic = importedMnemonic ?: return
        val passphrase = passphrase?.takeIf { it.isNotEmpty() }
        importWallet(mnemonic, passphrase)
    }

    private fun addSuggestedWord(word: String) {
        updateUiState { st ->
            val text = st.words.text
            val wordsFromText = text.split(" ").filter { it.isNotBlank() }.map { it.trim() }
            val newWords = wordsFromText.dropLast(1) + word
            val newWordsText = newWords.joinToString(" ")
            st.copy(
                words = TextFieldValue(
                    text = newWordsText,
                    selection = TextRange(newWordsText.length),
                ),
            )
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
        if (lastWord.length < 2) return

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
            delay(timeMillis = 500)
            interceptWords(wordsField)
        }.saveIn(wordsCheckJobHolder)
    }

    private fun interceptWords(wordsField: TextFieldValue) {
        updateUiState {
            it.copy(
                createWalletEnabled = false,
                wordsErrorText = null,
            )
        }

        importedMnemonic = null

        val text = wordsField.text
        val wordsFromText = text.split(" ").filter { it.isNotBlank() }.map { it.trim() }
        val invalidWords = wordsFromText.filterNot { it in mnemonicRepository.words }
        if (invalidWords.isNotEmpty()) {
            updateUiState {
                it.copy(
                    invalidWords = invalidWords.toImmutableList(),
                    wordsErrorText = resourceReference(R.string.onboarding_seed_mnemonic_wrong_words),
                    createWalletEnabled = false,
                )
            }
            return
        }

        try {
            val mnemonic = mnemonicRepository.generateMnemonic(text)
            importedMnemonic = mnemonic
            updateUiState {
                it.copy(
                    invalidWords = emptyList<String>().toImmutableList(),
                    wordsErrorText = null,
                    createWalletEnabled = true,
                )
            }
        } catch (ex: TangemSdkError.MnemonicException) {
            val error = ex.mnemonicResult
            if (error is MnemonicErrorResult.InvalidChecksum) {
                updateUiState {
                    it.copy(
                        wordsErrorText = resourceReference(R.string.onboarding_seed_mnemonic_invalid_checksum),
                        createWalletEnabled = false,
                    )
                }
            } else {
                updateUiState {
                    it.copy(
                        createWalletEnabled = false,
                        wordsErrorText = null,
                    )
                }
            }
        }
    }

    private fun showInfoBS() {
        updateUiState { state ->
            state.copy(
                infoBottomSheetConfig = TangemBottomSheetConfig.Empty.copy(
                    isShow = true,
                    onDismissRequest = {
                        updateUiState { it.copy(infoBottomSheetConfig = TangemBottomSheetConfig.Empty) }
                    },
                ),
            )
        }
    }
}