package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onboarding.v2.common.ui.OnboardingDialogUM
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.GeneratedWordsType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class MultiWalletSeedPhraseUM(
    val order: Int,
) {

    data class Start(
        val onLearnMoreClicked: () -> Unit = {},
        val onGenerateSeedPhraseClicked: () -> Unit = {},
        val onImportSeedPhraseClicked: () -> Unit = {},
    ) : MultiWalletSeedPhraseUM(order = 0)

    data class GenerateSeedPhrase(
        val option: GeneratedWordsType = GeneratedWordsType.Words12,
        val words: ImmutableList<MnemonicGridItem> = persistentListOf(),
        val onOptionChange: (GeneratedWordsType) -> Unit = {},
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
        val dialog: OnboardingDialogUM? = null,
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
        val onPassphraseInfoClick: () -> Unit = {},
        val wordsErrorText: TextReference? = null,
        val invalidWords: ImmutableList<String> = persistentListOf(),
        val createWalletEnabled: Boolean = false,
        val createWalletProgress: Boolean = false,
        val createWalletClick: () -> Unit = {},
        val suggestionsList: ImmutableList<String> = persistentListOf(),
        val onSuggestionClick: (String) -> Unit = {},
        val infoBottomSheetConfig: TangemBottomSheetConfig = TangemBottomSheetConfig.Empty,
        val dialog: OnboardingDialogUM? = null,
    ) : MultiWalletSeedPhraseUM(order = 1)
}