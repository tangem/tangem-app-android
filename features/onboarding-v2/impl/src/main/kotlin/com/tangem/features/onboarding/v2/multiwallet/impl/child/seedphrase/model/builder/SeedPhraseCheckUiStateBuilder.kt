package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.GeneratedWordsType
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.SeedPhraseState
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import kotlinx.collections.immutable.toImmutableList

internal class SeedPhraseCheckUiStateBuilder(
    private val currentState: () -> SeedPhraseState,
    private val currentUiState: () -> MultiWalletSeedPhraseUM,
    private val updateUiState: (
        (MultiWalletSeedPhraseUM.GeneratedWordsCheck) -> MultiWalletSeedPhraseUM.GeneratedWordsCheck,
    ) -> Unit,
    private val importWallet: () -> Unit,
    private val readyToImport: (Boolean) -> Unit,
) {

    @Suppress("MagicNumber")
    fun getState(): MultiWalletSeedPhraseUM.GeneratedWordsCheck {
        val wordFields = List(3) { index ->
            val shownIndex = when (index) {
                0 -> 2
                1 -> 7
                2 -> 11
                else -> error("")
            }

            MultiWalletSeedPhraseUM.GeneratedWordsCheck.WordField(
                index = shownIndex,
                word = TextFieldValue(""),
                error = false,
                onChange = { changeField ->
                    updateUiState { st ->
                        val newState = st.copy(
                            wordFields = st.wordFields.map { wordField ->
                                if (wordField.index == shownIndex) {
                                    wordField.updateField(changeField, shownIndex)
                                } else {
                                    wordField
                                }
                            }.toImmutableList(),
                        )

                        val allFieldsCorrect = newState.allFieldsCorrect()
                        readyToImport(allFieldsCorrect)

                        newState.copy(
                            createWalletButtonEnabled = allFieldsCorrect,
                        )
                    }
                },
            )
        }.toImmutableList()

        return MultiWalletSeedPhraseUM.GeneratedWordsCheck(
            wordFields = wordFields,
            createWalletButtonEnabled = false,
            createWalletButtonProgress = false,
            onCreateWalletButtonClick = {
                val currentState = currentUiState() as? MultiWalletSeedPhraseUM.GeneratedWordsCheck
                    ?: return@GeneratedWordsCheck
                if (currentState.createWalletButtonEnabled) {
                    importWallet()
                }
            },
        )
    }

    private fun MultiWalletSeedPhraseUM.GeneratedWordsCheck.WordField.updateField(
        newText: TextFieldValue,
        shownIndex: Int,
    ): MultiWalletSeedPhraseUM.GeneratedWordsCheck.WordField {
        val correct = checkWordField(word = newText.text, shownIndex = shownIndex)

        return copy(
            word = newText,
            error = correct.not(),
        )
    }

    private fun MultiWalletSeedPhraseUM.GeneratedWordsCheck.allFieldsCorrect(): Boolean {
        return wordFields.all {
            checkWordField(
                word = it.word.text,
                shownIndex = it.index,
            )
        }
    }

    private fun checkWordField(word: String, shownIndex: Int): Boolean {
        val currentState = currentState()

        currentState.generatedWords12 ?: return false
        currentState.generatedWords24 ?: return false

        val wordList = when (currentState.generatedWordsType) {
            GeneratedWordsType.Words12 -> currentState.generatedWords12
            GeneratedWordsType.Words24 -> currentState.generatedWords24
        }.mnemonicComponents

        return wordList[shownIndex - 1] == word
    }
}