package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.event.StateEvent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.SendFeeNotification
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

/**
 * Ui states of the send screen
 */
@Immutable
internal data class SendUiState(
    val clickIntents: SendClickIntents,
    val amountState: SendStates.AmountState? = null,
    val recipientState: SendStates.RecipientState? = null,
    val feeState: SendStates.FeeState? = null,
    val sendState: SendStates.SendState = SendStates.SendState(),
    val recipientList: MutableStateFlow<PagingData<SendRecipientListContent>> = MutableStateFlow(PagingData.empty()),
    val currentState: MutableStateFlow<SendUiStateType>,
    val event: StateEvent<SendEvent>,
)

@Stable
internal sealed class SendStates {

    abstract val type: SendUiStateType

    abstract val isPrimaryButtonEnabled: Boolean

    /** Amount state */
    data class AmountState(
        override val type: SendUiStateType = SendUiStateType.Amount,
        override val isPrimaryButtonEnabled: Boolean,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrency: AppCurrency,
        val walletName: String,
        val walletBalance: String,
        val tokenIconState: TokenIconState,
        val isFiatValue: Boolean,
        val segmentedButtonConfig: PersistentList<SendAmountSegmentedButtonsConfig>,
        val amountTextField: SendTextField.Amount,
    ) : SendStates()

    /** Recipient state */
    data class RecipientState(
        override val type: SendUiStateType = SendUiStateType.Recipient,
        override val isPrimaryButtonEnabled: Boolean,
        val addressTextField: SendTextField.RecipientAddress,
        val memoTextField: SendTextField.RecipientMemo?,
        val recipients: MutableStateFlow<PagingData<SendRecipientListContent>> = MutableStateFlow(PagingData.empty()),
        val network: String,
        val isValidating: Boolean = false,
    ) : SendStates()

    /** Fee and speed state */
    data class FeeState(
        override val type: SendUiStateType = SendUiStateType.Fee,
        override val isPrimaryButtonEnabled: Boolean = false,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeSelectorState: FeeSelectorState = FeeSelectorState.Loading,
        val isSubtract: Boolean = false,
        val fee: Fee? = null,
        val receivedAmountValue: BigDecimal = BigDecimal.ZERO,
        val receivedAmount: String = "",
        val notifications: ImmutableList<SendFeeNotification> = persistentListOf(),
    ) : SendStates()

    /** Send state */
    data class SendState(
        override val type: SendUiStateType = SendUiStateType.Send,
        override val isPrimaryButtonEnabled: Boolean = true,
        val isSending: Boolean = false,
        val isSuccess: Boolean = false,
        val transactionDate: Long = 0L,
        val txUrl: String = "",
        val notifications: ImmutableList<SendNotification> = persistentListOf(),
    ) : SendStates()
}

enum class SendUiStateType {
    Amount,
    Recipient,
    Fee,
    Send,
}