package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Ui states of the send screen
 */
@Immutable
internal data class SendUiState(
    val clickIntents: SendClickIntents,
    val amountState: SendStates.AmountState? = null,
    val recipientState: SendStates.RecipientState? = null,
    val feeState: SendStates.FeeState? = null,
    val recipientList: MutableStateFlow<PagingData<SendRecipientListContent>> = MutableStateFlow(PagingData.empty()),
    val currentState: MutableStateFlow<SendUiStateType>,
)

@Stable
internal sealed class SendStates {

    abstract val type: SendUiStateType

    /** Amount state */
    data class AmountState(
        override val type: SendUiStateType = SendUiStateType.Amount,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrency: AppCurrency,
        val walletName: String,
        val walletBalance: String,
        val tokenIconState: TokenIconState,
        val isFiatValue: Boolean,
        val segmentedButtonConfig: PersistentList<SendAmountSegmentedButtonsConfig>,
        val amountTextField: MutableStateFlow<SendTextField.Amount>,
        val isPrimaryButtonEnabled: Boolean,
    ) : SendStates()

    /** Recipient state */
    data class RecipientState(
        override val type: SendUiStateType = SendUiStateType.Recipient,
        val addressTextField: MutableStateFlow<SendTextField.RecipientAddress>,
        val memoTextField: MutableStateFlow<SendTextField.RecipientMemo>?,
        val recipients: MutableStateFlow<PagingData<SendRecipientListContent>> = MutableStateFlow(PagingData.empty()),
        val network: String,
        val isPrimaryButtonEnabled: Boolean,
    ) : SendStates()
// [REDACTED_TODO_COMMENT]
    /** Fee and speed state */
    data class FeeState(
        override val type: SendUiStateType = SendUiStateType.Fee,
    ) : SendStates()
// [REDACTED_TODO_COMMENT]
    /** Send state */
    data class SendState(
        val isSuccess: Boolean,
    )
}

enum class SendUiStateType {
    Amount,
    Recipient,
    Fee,
    Send,
    Done,
}
