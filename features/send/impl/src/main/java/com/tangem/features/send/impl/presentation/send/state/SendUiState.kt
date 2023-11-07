package com.tangem.features.send.impl.presentation.send.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.send.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.send.viewmodel.SendClickIntents
import kotlinx.collections.immutable.PersistentList

/**
 * Ui states of the send screen
 */
@Immutable
internal sealed class SendUiState {

    /** States with content */
    sealed class Content : SendUiState() {

        /** Is primary button enabled */
        abstract val isPrimaryButtonEnabled: Boolean

        /** Click intents */
        abstract val clickIntents: SendClickIntents

        /** Initial state */
        data class Initial(
            override val isPrimaryButtonEnabled: Boolean = false,
            override val clickIntents: SendClickIntents,
        ) : Content()

        /** Amount state */
        data class AmountState(
            override val isPrimaryButtonEnabled: Boolean = false,
            override val clickIntents: SendClickIntents,
            val walletName: String,
            val walletBalance: String,
            val tokenIconState: TokenIconState,
            val cryptoCurrencyStatus: CryptoCurrencyStatus,
            val appCurrency: AppCurrency,
            val isFiatValue: Boolean,
            val segmentedButtonConfig: PersistentList<SendAmountSegmentedButtonsConfig>,
            val amountTextField: SendTextField.Amount,
        ) : Content()

        // todo [REDACTED_JIRA]
        /** Recipient state */
        data class RecipientState(
            override val isPrimaryButtonEnabled: Boolean = false,
            override val clickIntents: SendClickIntents,
        ) : Content()

        // todo [REDACTED_JIRA]
        /** Fee and speed state */
        data class FeeState(
            override val isPrimaryButtonEnabled: Boolean = false,
            override val clickIntents: SendClickIntents,
        ) : Content()

        // todo [REDACTED_JIRA]
        /** Send state */
        data class SendState(
            override val isPrimaryButtonEnabled: Boolean = true,
            override val clickIntents: SendClickIntents,
        ) : Content()
    }

    /** Dismiss screen */
    object Dismiss : SendUiState()
}