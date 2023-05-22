package com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders

import com.tangem.feature.onboarding.presentation.wallet2.model.*
import kotlinx.collections.immutable.ImmutableList

/**
[REDACTED_AUTHOR]
 */
class StateBuilder(
    private val uiActions: UiActions,
) {

    val checkSeedPhrase: CheckSeedPhraseStateBuilder = CheckSeedPhraseStateBuilder()
    val importSeedPhrase: ImportSeedPhraseStateBuilder = ImportSeedPhraseStateBuilder()

    fun init(): OnboardingSeedPhraseState = OnboardingSeedPhraseState(
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
        onBackClick = uiActions.menuNavigateBackClick,
    )

    fun generateMnemonicComponents(uiState: OnboardingSeedPhraseState): OnboardingSeedPhraseState {
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