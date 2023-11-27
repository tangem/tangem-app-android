package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
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
    val sendState: SendStates.SendState? = null,
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

    /** Fee and speed state */
    data class FeeState(
        override val type: SendUiStateType = SendUiStateType.Fee,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeSelectorState: MutableStateFlow<FeeSelectorState> = MutableStateFlow(FeeSelectorState.Empty),
        val isSubtract: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val receivedAmount: MutableStateFlow<String> = MutableStateFlow(""),
    ) : SendStates()

    /** Send state */
    data class SendState(
        override val type: SendUiStateType = SendUiStateType.Send,
        val isSending: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val isSuccess: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val transactionDate: MutableStateFlow<Long> = MutableStateFlow(0L),
        val txUrl: MutableStateFlow<String> = MutableStateFlow(""),
    ) : SendStates()
}

enum class SendUiStateType {
    Amount,
    Recipient,
    Fee,
    Send,
}
