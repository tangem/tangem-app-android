package com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders

import com.tangem.feature.onboarding.presentation.wallet2.model.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
class StateBuilder(
    private val uiActions: UiActions,
) {

    val checkSeedPhrase: CheckSeedPhraseStateBuilder = CheckSeedPhraseStateBuilder()
    val importSeedPhrase: ImportSeedPhraseStateBuilder = ImportSeedPhraseStateBuilder()

    fun init(selectedSeedType: SegmentSeedType): OnboardingSeedPhraseState = OnboardingSeedPhraseState(
        introState = getInitialIntroState(),
        aboutState = getInitialAboutState(),
        yourSeedPhraseState = getInitialYourSeedPhraseState(selectedSeedType),
        checkSeedPhraseState = getInitialCheckSeedPhraseState(),
        importSeedPhraseState = getInitialImportSeedPhraseState(),
        menuButtonChat = ButtonState(
            onClick = uiActions.menuChatClick,
        ),
        onBackClick = uiActions.menuNavigateBackClick,
    )

    private fun getInitialImportSeedPhraseState(): ImportSeedPhraseState {
        return ImportSeedPhraseState(
            fieldSeedPhrase = TextFieldState(
                onTextFieldValueChanged = uiActions.importSeedPhraseActions.phraseTextFieldAction.onTextFieldChanged,
                onFocusChanged = uiActions.importSeedPhraseActions.phraseTextFieldAction.onFocusChanged,
            ),
            fieldPassphrase = TextFieldState(
                onTextFieldValueChanged = uiActions.importSeedPhraseActions.passTextFieldAction.onTextFieldChanged,
                onFocusChanged = uiActions.importSeedPhraseActions.passTextFieldAction.onFocusChanged,
            ),
            onSuggestedPhraseClick = uiActions.importSeedPhraseActions.suggestedPhraseClick,
            onPassphraseInfoClick = uiActions.importSeedPhraseActions.onPassphraseInfoClick,
            buttonCreateWallet = ButtonState(
                enabled = false,
                onClick = uiActions.importSeedPhraseActions.buttonCreateWalletClick,
            ),
        )
    }

    private fun getInitialCheckSeedPhraseState(): CheckSeedPhraseState {
        return CheckSeedPhraseState(
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
        )
    }

    private fun getInitialYourSeedPhraseState(selectedSeedType: SegmentSeedType): YourSeedPhraseState {
        return YourSeedPhraseState(
            segmentSeedState = SegmentSeedState(
                seedSegments = persistentListOf(
                    SegmentSeedType.SEED_12,
                    SegmentSeedType.SEED_24,
                ),
                selectedSeedType = selectedSeedType,
                onSelectType = uiActions.yourSeedPhraseActions.onSelectType,
            ),
            buttonContinue = ButtonState(
                onClick = uiActions.yourSeedPhraseActions.buttonContinueClick,
            ),
        )
    }

    private fun getInitialAboutState(): AboutState {
        return AboutState(
            buttonReadMoreAboutSeedPhrase = ButtonState(
                onClick = uiActions.aboutActions.buttonReadMoreAboutSeedPhraseClick,
            ),
            buttonGenerateSeedPhrase = ButtonState(
                onClick = uiActions.aboutActions.buttonGenerateSeedPhraseClick,
            ),
            buttonImportSeedPhrase = ButtonState(
                onClick = uiActions.aboutActions.buttonImportSeedPhraseClick,
            ),
        )
    }

    private fun getInitialIntroState(): IntroState {
        return IntroState(
            buttonCreateWallet = ButtonState(
                onClick = uiActions.introActions.buttonCreateWalletClick,
            ),
            buttonOtherOptions = ButtonState(
                onClick = uiActions.introActions.buttonOtherOptionsClick,
            ),
        )
    }

    fun setCardArtwork(uiState: OnboardingSeedPhraseState, cardArtwork: String): OnboardingSeedPhraseState =
        uiState.copy(
            introState = uiState.introState.copy(
                cardImageUrl = cardArtwork,
            ),
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
        mnemonicGridItems: PersistentList<MnemonicGridItem>,
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

    fun selectSeedType(
        uiState: OnboardingSeedPhraseState,
        mnemonicGridItems: PersistentList<MnemonicGridItem>,
        selectedSeedType: SegmentSeedType,
    ): OnboardingSeedPhraseState = uiState.copy(
        yourSeedPhraseState = uiState.yourSeedPhraseState.copy(
            mnemonicGridItems = mnemonicGridItems,
            segmentSeedState = uiState.yourSeedPhraseState.segmentSeedState.copy(
                selectedSeedType = selectedSeedType,
            ),
        ),
    )

    fun clearCheckSeedPhraseState(uiState: OnboardingSeedPhraseState): OnboardingSeedPhraseState = uiState.copy(
        checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
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
        ),
    )

    private fun updateMnemonicComponents(
        uiState: OnboardingSeedPhraseState,
        mnemonicGridItems: PersistentList<MnemonicGridItem>,
    ): OnboardingSeedPhraseState = uiState.copy(
        yourSeedPhraseState = uiState.yourSeedPhraseState.copy(
            mnemonicGridItems = mnemonicGridItems,
        ),
    )
}