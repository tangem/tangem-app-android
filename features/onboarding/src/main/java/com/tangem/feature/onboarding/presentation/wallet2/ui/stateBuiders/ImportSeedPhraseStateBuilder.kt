package com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.onboarding.domain.InsertSuggestionResult
import com.tangem.feature.onboarding.domain.SeedPhraseError
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState

/**
* [REDACTED_AUTHOR]
 */
class ImportSeedPhraseStateBuilder {

    fun updateTextField(
        uiState: OnboardingSeedPhraseState,
        textFieldValue: TextFieldValue,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            tvSeedPhrase = uiState.importSeedPhraseState.tvSeedPhrase.copy(
                textFieldValue = textFieldValue,
            ),
        ),
    )

    fun updateCreateWalletButton(
        uiState: OnboardingSeedPhraseState,
        enabled: Boolean,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                buttonCreateWallet = uiState.importSeedPhraseState.buttonCreateWallet.copy(
                    enabled = enabled,
                ),
            ),
        )
    }

    fun updateInvalidWords(
        uiState: OnboardingSeedPhraseState,
        invalidWords: Set<String>,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            invalidWords = invalidWords,
        ),
    )

    fun updateSuggestions(
        uiState: OnboardingSeedPhraseState,
        suggestions: List<String>,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            suggestionsList = suggestions,
        ),
    )

    fun insertSuggestionWord(
        uiState: OnboardingSeedPhraseState,
        insertResult: InsertSuggestionResult,
    ): OnboardingSeedPhraseState {
        val newTextFieldValue = uiState.importSeedPhraseState.tvSeedPhrase.textFieldValue.copy(
            text = insertResult.text,
            selection = TextRange(insertResult.cursorPosition, insertResult.cursorPosition),
        )
        return updateTextField(uiState, newTextFieldValue)
    }

    fun updateError(
        uiState: OnboardingSeedPhraseState,
        error: SeedPhraseError?,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                error = error,
            ),
        )
    }
}
