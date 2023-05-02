package com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.SeedPhraseField

/**
[REDACTED_AUTHOR]
 */
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

    fun updateCreateWalletButton(uiState: OnboardingSeedPhraseState, enabled: Boolean): OnboardingSeedPhraseState {
        return uiState.copy(
            checkSeedPhraseState = uiState.checkSeedPhraseState.copy(
                buttonCreateWallet = uiState.checkSeedPhraseState.buttonCreateWallet.copy(
                    enabled = enabled,
                ),
            ),
        )
    }
}