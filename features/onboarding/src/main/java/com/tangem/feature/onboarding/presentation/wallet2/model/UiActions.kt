package com.tangem.feature.onboarding.presentation.wallet2.model

import androidx.compose.ui.text.input.TextFieldValue

/**
[REDACTED_AUTHOR]
 */
data class UiActions(
    val introActions: IntroUiAction,
    val aboutActions: AboutUiAction,
    val yourSeedPhraseActions: YourSeedPhraseUiAction,
    val checkSeedPhraseActions: CheckSeedPhraseUiAction,
    val importSeedPhraseActions: ImportSeedPhraseUiAction,
    val menuChatClick: () -> Unit,
)

data class IntroUiAction(
    val buttonCreateWalletClick: () -> Unit,
    val buttonOtherOptionsClick: () -> Unit,
)

data class AboutUiAction(
    val buttonReadMoreAboutSeedPhraseClick: () -> Unit,
    val buttonGenerateSeedPhraseClick: () -> Unit,
    val buttonImportSeedPhraseClick: () -> Unit,
)

data class YourSeedPhraseUiAction(
    val buttonContinueClick: () -> Unit,
)

data class CheckSeedPhraseUiAction(
    val secondTextFieldAction: TextFieldUiAction,
    val seventhTextFieldAction: TextFieldUiAction,
    val eleventhTextFieldAction: TextFieldUiAction,
    val buttonCreateWalletClick: () -> Unit,
)

data class ImportSeedPhraseUiAction(
    val phraseTextFieldAction: TextFieldUiAction,
    val suggestedPhraseClick: (Int) -> Unit,
    val buttonCreateWalletClick: () -> Unit,
)

data class TextFieldUiAction(
    val onFocusChanged: (Boolean) -> Unit,
    val onTextFieldChanged: (TextFieldValue) -> Unit,
)

enum class SeedPhraseField {
    Second,
    Seventh,
    Eleventh,
}