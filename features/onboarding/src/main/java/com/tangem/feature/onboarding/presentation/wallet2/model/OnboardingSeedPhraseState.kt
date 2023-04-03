package com.tangem.feature.onboarding.presentation.wallet2.model

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.onboarding.domain.SeedPhraseError

/**
* [REDACTED_AUTHOR]
 */
data class OnboardingSeedPhraseState(
    val step: OnboardingSeedPhraseStep = OnboardingSeedPhraseStep.Intro,
    val introState: IntroState = IntroState(),
    val aboutState: AboutState = AboutState(),
    val yourSeedPhraseState: YourSeedPhraseState = YourSeedPhraseState(),
    val checkSeedPhraseState: CheckSeedPhraseState = CheckSeedPhraseState(),
    val importSeedPhraseState: ImportSeedPhraseState = ImportSeedPhraseState(),
    val menuButtonChat: ButtonState = ButtonState(),
    val isOnboardingFinished: Boolean = false,
)

enum class OnboardingSeedPhraseStep {
    Intro, AboutSeedPhrase, YourSeedPhrase, CheckSeedPhrase, ImportSeedPhrase
}

data class IntroState(
    val buttonCreateWallet: ButtonState = ButtonState(),
    val buttonOtherOptions: ButtonState = ButtonState(),
)

data class AboutState(
    val buttonReadMoreAboutSeedPhrase: ButtonState = ButtonState(),
    val buttonGenerateSeedPhrase: ButtonState = ButtonState(),
    val buttonImportSeedPhrase: ButtonState = ButtonState(),
)

data class YourSeedPhraseState(
    val mnemonicComponents: List<String> = listOf(),
    val buttonContinue: ButtonState = ButtonState(),
)

data class CheckSeedPhraseState(
    val tvSecondPhrase: TextFieldState = TextFieldState(),
    val tvSeventhPhrase: TextFieldState = TextFieldState(),
    val tvEleventhPhrase: TextFieldState = TextFieldState(),
    val buttonCreateWallet: ButtonState = ButtonState(),
)

data class ImportSeedPhraseState(
    val tvSeedPhrase: TextFieldState = TextFieldState(),
    val invalidWords: Set<String> = emptySet(),
    val suggestionsList: List<String> = emptyList(),
    val error: SeedPhraseError? = null,
    val onSuggestedPhraseClick: (Int) -> Unit = {},
    val buttonCreateWallet: ButtonState = ButtonState(),
)

data class TextFieldState(
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val label: String? = null,
    @StringRes val labelRes: Int? = null,
    val isError: Boolean = false,
    val isFocused: Boolean = false,
    val onTextFieldValueChanged: (TextFieldValue) -> Unit = {},
    val onFocusChanged: (Boolean) -> Unit = {},
)
