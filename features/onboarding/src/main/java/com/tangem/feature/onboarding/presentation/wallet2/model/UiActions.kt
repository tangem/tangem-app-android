package com.tangem.feature.onboarding.presentation.wallet2.model

import androidx.compose.ui.text.input.TextFieldValue

/**
[REDACTED_AUTHOR]
 */
data class UiActions(
    val introActions: IntroUiAction = IntroUiAction(),
    val aboutActions: AboutUiAction = AboutUiAction(),
    val yourSeedPhraseActions: YourSeedPhraseUiAction = YourSeedPhraseUiAction(),
    val checkSeedPhraseActions: CheckSeedPhraseUiAction = CheckSeedPhraseUiAction(),
    val importSeedPhraseActions: ImportSeedPhraseUiAction = ImportSeedPhraseUiAction(),
    val menuChatClick: () -> Unit = {},
)

data class IntroUiAction(
    val buttonCreateWalletClick: () -> Unit = {},
    val buttonOtherOptionsClick: () -> Unit = {},
)

data class AboutUiAction(
    val buttonReadMoreAboutSeedPhraseClick: () -> Unit = {},
    val buttonGenerateSeedPhraseClick: () -> Unit = {},
    val buttonImportSeedPhraseClick: () -> Unit = {},
)

data class YourSeedPhraseUiAction(
    val buttonContinueClick: () -> Unit = {},
)

data class CheckSeedPhraseUiAction(
    val secondTextFieldAction: TextFieldUiAction = TextFieldUiAction(),
    val seventhTextFieldAction: TextFieldUiAction = TextFieldUiAction(),
    val eleventhTextFieldAction: TextFieldUiAction = TextFieldUiAction(),
    val buttonCreateWalletClick: () -> Unit = {},
)

data class ImportSeedPhraseUiAction(
    val phraseTextFieldAction: TextFieldUiAction = TextFieldUiAction(),
    val suggestedPhraseClick: (Int) -> Unit = {},
    val buttonCreateWalletClick: () -> Unit = {},
)

data class TextFieldUiAction(
    val onFocusChanged: (Boolean) -> Unit = { },
    val onTextFieldChanged: (TextFieldValue) -> Unit = { },
)

enum class SeedPhraseField {
    Second,
    Seventh,
    Eleventh,
}