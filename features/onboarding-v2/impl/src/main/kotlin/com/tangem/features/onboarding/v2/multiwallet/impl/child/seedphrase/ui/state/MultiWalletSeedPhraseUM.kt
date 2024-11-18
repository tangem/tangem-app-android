package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed class MultiWalletSeedPhraseUM(
    val order: Int,
) {

    data class Start(
        val onLearnMoreClicked: () -> Unit = {},
        val onGenerateSeedPhraseClicked: () -> Unit = {},
        val onImportSeedPhraseClicked: () -> Unit = {},
    ) : MultiWalletSeedPhraseUM(order = 0)

    data class GenerateSeedPhrase(
        val words24OptionSelected: Boolean = false,
        val words: ImmutableList<MnemonicGridItem> = persistentListOf(),
        val onWords24OptionSwitch: () -> Unit = {},
        val onContinueClick: () -> Unit = {},
    ) : MultiWalletSeedPhraseUM(order = 1) {
        data class MnemonicGridItem(
            val index: Int,
            val mnemonic: String,
        )
    }

    data class GeneratedWordsCheck(
        val wordFields: ImmutableList<WordField> = persistentListOf(),
        val createWalletButtonEnabled: Boolean = false,
        val createWalletButtonProgress: Boolean = false,
        val onCreateWalletButtonClick: () -> Unit = {},
    ) : MultiWalletSeedPhraseUM(order = 2) {
        data class WordField(
            val index: Int,
            val word: TextFieldValue,
            val error: Boolean,
            val onChange: (TextFieldValue) -> Unit,
        )
    }

    data class Import(
        val words: TextFieldValue = TextFieldValue(""),
        val wordsChange: (TextFieldValue) -> Unit = {},
        val passPhrase: TextFieldValue = TextFieldValue(""),
        val passPhraseChange: (TextFieldValue) -> Unit = {},
        val wordsError: Boolean = false,
        val createWalletEnabled: Boolean = false,
        val createWalletClick: () -> Unit = {},
    ) : MultiWalletSeedPhraseUM(order = 1)
}
