package com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders

import com.tangem.feature.onboarding.presentation.wallet2.model.AboutState
import com.tangem.feature.onboarding.presentation.wallet2.model.ButtonState
import com.tangem.feature.onboarding.presentation.wallet2.model.CheckSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.ImportSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.IntroState
import com.tangem.feature.onboarding.presentation.wallet2.model.MnemonicGridItem
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseStep
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldState
import com.tangem.feature.onboarding.presentation.wallet2.model.UiActions
import com.tangem.feature.onboarding.presentation.wallet2.model.YourSeedPhraseState
import kotlinx.collections.immutable.ImmutableList

/**
* [REDACTED_AUTHOR]
 */
class StateBuilder(
    private val uiActions: UiActions,
) {

    val checkSeedPhrase: CheckSeedPhraseStateBuilder = CheckSeedPhraseStateBuilder()
    val importSeedPhrase: ImportSeedPhraseStateBuilder = ImportSeedPhraseStateBuilder()

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
            tvSeedPhrase = TextFieldState(
                onTextFieldValueChanged = uiActions.importSeedPhraseActions.phraseTextFieldAction.onTextFieldChanged,
                onFocusChanged = uiActions.importSeedPhraseActions.phraseTextFieldAction.onFocusChanged,
            ),
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

    fun generateMnemonicComponents(
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

    fun mnemonicGenerated(
        uiState: OnboardingSeedPhraseState,
        mnemonicGridItems: ImmutableList<MnemonicGridItem>,
    ): OnboardingSeedPhraseState {
        return updateMnemonicComponents(uiState, mnemonicGridItems)
            .copy(
                step = OnboardingSeedPhraseStep.YourSeedPhrase,
                aboutState = uiState.aboutState.copy(
                    buttonGenerateSeedPhrase = uiState.aboutState.buttonGenerateSeedPhrase.copy(
                        showProgress = false,
                    ),
                    buttonImportSeedPhrase = uiState.aboutState.buttonImportSeedPhrase.copy(
                        enabled = true,
                    ),
                ),
            )
    }

    fun updateMnemonicComponents(
        uiState: OnboardingSeedPhraseState,
        mnemonicGridItems: ImmutableList<MnemonicGridItem>,
    ): OnboardingSeedPhraseState = uiState.copy(
        yourSeedPhraseState = uiState.yourSeedPhraseState.copy(
            mnemonicGridItems = mnemonicGridItems,
        ),
    )
}
