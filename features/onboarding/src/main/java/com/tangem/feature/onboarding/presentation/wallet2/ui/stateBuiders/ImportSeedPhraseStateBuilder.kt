package com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.feature.onboarding.domain.InsertSuggestionResult
import com.tangem.feature.onboarding.domain.SeedPhraseError
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.ShowPassphraseInfoBottomSheetContent
import kotlinx.collections.immutable.ImmutableList

/**
[REDACTED_AUTHOR]
 */
class ImportSeedPhraseStateBuilder {

    fun updateSeedPhraseTextField(
        uiState: OnboardingSeedPhraseState,
        textFieldValue: TextFieldValue,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            fieldSeedPhrase = uiState.importSeedPhraseState.fieldSeedPhrase.copy(
                textFieldValue = textFieldValue,
            ),
        ),
    )

    fun updatePassPhraseTextField(
        uiState: OnboardingSeedPhraseState,
        textFieldValue: TextFieldValue,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            fieldPassphrase = uiState.importSeedPhraseState.fieldPassphrase.copy(
                textFieldValue = textFieldValue,
            ),
        ),
    )

    fun updateCreateWalletButton(uiState: OnboardingSeedPhraseState, enabled: Boolean): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                buttonCreateWallet = uiState.importSeedPhraseState.buttonCreateWallet.copy(
                    enabled = enabled,
                ),
            ),
        )
    }

    fun updateInvalidWords(uiState: OnboardingSeedPhraseState, invalidWords: Set<String>): OnboardingSeedPhraseState =
        uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                invalidWords = invalidWords,
            ),
        )

    fun updateSuggestions(
        uiState: OnboardingSeedPhraseState,
        suggestions: ImmutableList<String>,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            suggestionsList = suggestions,
        ),
    )

    fun showPassphraseInfoBottomSheet(
        uiState: OnboardingSeedPhraseState,
        onDismiss: () -> Unit,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = onDismiss,
                    content = ShowPassphraseInfoBottomSheetContent(onDismiss),
                ),
            ),
        )
    }

    fun dismissPassphraseBottomSheet(uiState: OnboardingSeedPhraseState): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                bottomSheetConfig = uiState.importSeedPhraseState.bottomSheetConfig?.copy(
                    isShown = false,
                ),
            ),
        )
    }

    fun insertSuggestionWord(
        uiState: OnboardingSeedPhraseState,
        insertResult: InsertSuggestionResult,
    ): OnboardingSeedPhraseState {
        val newTextFieldValue = uiState.importSeedPhraseState.fieldSeedPhrase.textFieldValue.copy(
            text = insertResult.text,
            selection = TextRange(insertResult.cursorPosition, insertResult.cursorPosition),
        )
        return updateSeedPhraseTextField(uiState, newTextFieldValue)
    }

    fun updateError(uiState: OnboardingSeedPhraseState, error: SeedPhraseError?): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                error = error,
            ),
        )
    }
}