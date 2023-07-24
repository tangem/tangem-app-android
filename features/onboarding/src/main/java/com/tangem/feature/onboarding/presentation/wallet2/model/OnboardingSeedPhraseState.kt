package com.tangem.feature.onboarding.presentation.wallet2.model

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.onboarding.domain.SeedPhraseError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
* [REDACTED_AUTHOR]
 */
data class OnboardingSeedPhraseState(
    val introState: IntroState,
    val aboutState: AboutState,
    val yourSeedPhraseState: YourSeedPhraseState,
    val checkSeedPhraseState: CheckSeedPhraseState,
    val importSeedPhraseState: ImportSeedPhraseState,
    val menuButtonChat: ButtonState,
    val onBackClick: () -> Unit,
)

data class IntroState(
    val cardImageUrl: String? = null,
    val buttonCreateWallet: ButtonState,
    val buttonOtherOptions: ButtonState,
)

data class AboutState(
    val buttonReadMoreAboutSeedPhrase: ButtonState,
    val buttonGenerateSeedPhrase: ButtonState,
    val buttonImportSeedPhrase: ButtonState,
)

data class YourSeedPhraseState(
    val mnemonicGridItems: ImmutableList<MnemonicGridItem> = persistentListOf(),
    val buttonContinue: ButtonState,
)

data class MnemonicGridItem(
    val index: Int,
    val mnemonic: String,
)

data class CheckSeedPhraseState(
    val tvSecondPhrase: TextFieldState,
    val tvSeventhPhrase: TextFieldState,
    val tvEleventhPhrase: TextFieldState,
    val buttonCreateWallet: ButtonState,
)

data class ImportSeedPhraseState(
    val tvSeedPhrase: TextFieldState,
    val onSuggestedPhraseClick: (Int) -> Unit,
    val buttonCreateWallet: ButtonState,
    val invalidWords: Set<String> = emptySet(),
    val suggestionsList: ImmutableList<String> = persistentListOf(),
    val error: SeedPhraseError? = null,
)

data class TextFieldState(
    val onTextFieldValueChanged: (TextFieldValue) -> Unit,
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val label: String? = null,
    val isError: Boolean = false,
    val isFocused: Boolean = false,
    val onFocusChanged: (Boolean) -> Unit = {},
)
