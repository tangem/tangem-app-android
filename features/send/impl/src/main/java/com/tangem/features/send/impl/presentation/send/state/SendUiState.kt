package com.tangem.features.send.impl.presentation.send.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.send.state.fields.SendTextField
import kotlinx.collections.immutable.PersistentList

/**
 * Ui states of the send screen
 */
@Immutable
internal sealed class SendUiState {

    /** States with content */
    sealed class Content : SendUiState() {

        abstract val nextButtonEnabled: Boolean

        /** Initial state */
        data class Initial(
            override val nextButtonEnabled: Boolean = false,
        ) : Content()

        /** Amount state */
        data class AmountState(
            override val nextButtonEnabled: Boolean = false,
            val walletName: String,
            val walletBalance: String,
            val tokenIconState: TokenIconState,
            val cryptoCurrencyStatus: CryptoCurrencyStatus,
            val appCurrency: AppCurrency,
            val isFiatValue: Boolean,
            val segmentedButtonConfig: PersistentList<SendAmountSegmentedButtonConfig>,
            val amountTextField: SendTextField.Amount,
        ) : Content()
// [REDACTED_TODO_COMMENT]
        /** Recipient state */
        data class RecipientState(
            override val nextButtonEnabled: Boolean = false,
        ) : Content()
// [REDACTED_TODO_COMMENT]
        /** Fee and speed state */
        data class FeeState(
            override val nextButtonEnabled: Boolean = false,
        ) : Content()
// [REDACTED_TODO_COMMENT]
        /** Send state */
        data class SendState(
            override val nextButtonEnabled: Boolean = true,
        ) : Content()
    }
}
