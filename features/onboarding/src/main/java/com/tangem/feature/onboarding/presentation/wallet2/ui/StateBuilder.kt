package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.onboarding.presentation.wallet2.model.AboutState
import com.tangem.feature.onboarding.presentation.wallet2.model.ButtonState
import com.tangem.feature.onboarding.presentation.wallet2.model.CheckSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.ImportSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.IntroState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseStep
import com.tangem.feature.onboarding.presentation.wallet2.model.SeedPhraseField
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldState
import com.tangem.feature.onboarding.presentation.wallet2.model.UiActions
import com.tangem.feature.onboarding.presentation.wallet2.model.YourSeedPhraseState

/**
 * Created by Anton Zhilenkov on 14.03.2023.
 */
class StateBuilder(
    private val uiActions: UiActions,
) {

    val checkSeedPhrase: CheckSeedPhraseStateBuilder = CheckSeedPhraseStateBuilder()

    fun init(): OnboardingSeedPhraseState = OnboardingSeedPhraseState(
        step = OnboardingSeedPhraseStep.Intro,
        introState = IntroState(
            buttonCreateWallet = ButtonState(
                onClick = uiActions.introActions.buttonCreateWalletClick,
            ),
            buttonOtherOptions = ButtonState(
                onClick = uiActions.introActions.buttonOtherOptionsClick,
            ),
        ),
        aboutState = AboutState(
            buttonReadMoreAboutSeedPhrase = ButtonState(
                onClick = uiActions.aboutActions.buttonReadMoreAboutSeedPhraseClick,
            ),
            buttonGenerateSeedPhrase = ButtonState(
                onClick = uiActions.aboutActions.buttonGenerateSeedPhraseClick,
            ),
            buttonImportSeedPhrase = ButtonState(
                onClick = uiActions.aboutActions.buttonImportSeedPhraseClick,
            ),
        ),
        yourSeedPhraseState = YourSeedPhraseState(
            buttonContinue = ButtonState(
                onClick = uiActions.yourSeedPhraseActions.buttonContinueClick,
            ),
        ),
        checkSeedPhraseState = CheckSeedPhraseState(
            tvSecondPhrase = TextFieldState(
                label = "2",
                isFocused = true,
                onTextFieldValueChanged = uiActions.checkSeedPhraseActions.secondTextFieldAction.onTextFieldChanged,
                onFocusChanged = uiActions.checkSeedPhraseActions.secondTextFieldAction.onFocusChanged,
            ),
            tvSeventhPhrase = TextFieldState(
                label = "7",
                isFocused = false,
                onTextFieldValueChanged = uiActions.checkSeedPhraseActions.seventhTextFieldAction.onTextFieldChanged,
                onFocusChanged = uiActions.checkSeedPhraseActions.seventhTextFieldAction.onFocusChanged,
            ),
            tvEleventhPhrase = TextFieldState(
                label = "11",
                isFocused = false,
                onTextFieldValueChanged = uiActions.checkSeedPhraseActions.eleventhTextFieldAction.onTextFieldChanged,
                onFocusChanged = uiActions.checkSeedPhraseActions.eleventhTextFieldAction.onFocusChanged,
            ),
            buttonCreateWallet = ButtonState(
                enabled = false,
                onClick = uiActions.checkSeedPhraseActions.buttonCreateWalletClick,
            ),
        ),
        importSeedPhraseState = ImportSeedPhraseState(
            onSuggestedPhraseClick = uiActions.importSeedPhraseActions.suggestedPhraseClick,
            buttonCreateWallet = ButtonState(
                enabled = false,
                onClick = uiActions.importSeedPhraseActions.buttonCreateWalletClick,
            ),
        ),
        menuButtonChat = ButtonState(
            onClick = uiActions.menuChatClick,
        ),
    )

    fun changeStep(
        uiState: OnboardingSeedPhraseState,
        step: OnboardingSeedPhraseStep,
    ): OnboardingSeedPhraseState = uiState.copy(
        step = step,
    )

    fun generateSeedPhrase(
        uiState: OnboardingSeedPhraseState,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            aboutState = uiState.aboutState.copy(
                buttonGenerateSeedPhrase = uiState.aboutState.buttonGenerateSeedPhrase.copy(
                    showProgress = true,
                ),
                buttonImportSeedPhrase = uiState.aboutState.buttonImportSeedPhrase.copy(
                    enabled = false,
                ),
            ),
        )
    }

    fun seedPhraseGenerated(
        uiState: OnboardingSeedPhraseState,
        phraseList: List<String>,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            step = OnboardingSeedPhraseStep.YourSeedPhrase,
            aboutState = uiState.aboutState.copy(
                buttonGenerateSeedPhrase = uiState.aboutState.buttonGenerateSeedPhrase.copy(
                    showProgress = false,
                ),
                buttonImportSeedPhrase = uiState.aboutState.buttonImportSeedPhrase.copy(
                    enabled = true,
                ),
            ),
            yourSeedPhraseState = uiState.yourSeedPhraseState.copy(
                phraseList = phraseList,
            ),
        )
    }
}

class CheckSeedPhraseStateBuilder {

    fun updateTextField(
        uiState: OnboardingSeedPhraseState,
        field: SeedPhraseField,
        textFieldValue: TextFieldValue,
    ): OnboardingSeedPhraseState = when (field) {
        SeedPhraseField.Second -> uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                tvSecondPhrase = uiState.checkSeedPhraseState.tvSecondPhrase.copy(
                    textFieldValue = textFieldValue,
                ),
            ),
        )
        SeedPhraseField.Seventh -> uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                tvSeventhPhrase = uiState.checkSeedPhraseState.tvSeventhPhrase.copy(
                    textFieldValue = textFieldValue,
                ),
            ),
        )
        SeedPhraseField.Eleventh -> uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                tvEleventhPhrase = uiState.checkSeedPhraseState.tvEleventhPhrase.copy(
                    textFieldValue = textFieldValue,
                ),
            ),
        )
    }

    fun updateTextFieldError(
        uiState: OnboardingSeedPhraseState,
        field: SeedPhraseField,
        hasError: Boolean,
    ): OnboardingSeedPhraseState = when (field) {
        SeedPhraseField.Second -> uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                tvSecondPhrase = uiState.checkSeedPhraseState.tvSecondPhrase.copy(
                    isError = hasError,
                ),
            ),
        )
        SeedPhraseField.Seventh -> uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                tvSeventhPhrase = uiState.checkSeedPhraseState.tvSeventhPhrase.copy(
                    isError = hasError,
                ),
            ),
        )
        SeedPhraseField.Eleventh -> uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                tvEleventhPhrase = uiState.checkSeedPhraseState.tvEleventhPhrase.copy(
                    isError = hasError,
                ),
            ),
        )
    }

    fun updateCreateWalletButton(
        uiState: OnboardingSeedPhraseState,
        enabled: Boolean,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                buttonCreateWallet = uiState.checkSeedPhraseState.buttonCreateWallet.copy(
                    enabled = enabled,
                ),
            ),
        )
    }
}
