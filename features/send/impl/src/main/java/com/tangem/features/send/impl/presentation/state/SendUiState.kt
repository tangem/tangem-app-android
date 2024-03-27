package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.SendFeeNotification
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/**
 * Ui states of the send screen
 */
@Immutable
internal data class SendUiState(
    val clickIntents: SendClickIntents,
    val isEditingDisabled: Boolean,
    val cryptoCurrencySymbol: String,
    val amountState: SendStates.AmountState? = null,
    val recipientState: SendStates.RecipientState? = null,
    val feeState: SendStates.FeeState? = null,
    val sendState: SendStates.SendState = SendStates.SendState(),
    val isBalanceHidden: Boolean,
    val event: StateEvent<SendEvent>,
)

@Stable
internal sealed class SendStates {

    abstract val type: SendUiStateType

    abstract val isPrimaryButtonEnabled: Boolean

    /** Amount state */
    @Stable
    data class AmountState(
        override val type: SendUiStateType = SendUiStateType.Amount,
        override val isPrimaryButtonEnabled: Boolean,
        val walletName: String,
        val walletBalance: TextReference,
        val tokenIconState: TokenIconState,
        val segmentedButtonConfig: PersistentList<SendAmountSegmentedButtonsConfig>,
        val amountTextField: SendTextField.AmountField,
    ) : SendStates()

    /** Recipient state */
    @Stable
    data class RecipientState(
        override val type: SendUiStateType = SendUiStateType.Recipient,
        override val isPrimaryButtonEnabled: Boolean,
        val addressTextField: SendTextField.RecipientAddress,
        val memoTextField: SendTextField.RecipientMemo?,
        val recent: ImmutableList<SendRecipientListContent>,
        val wallets: ImmutableList<SendRecipientListContent>,
        val network: String,
        val isValidating: Boolean = false,
    ) : SendStates()

    /** Fee and speed state */
    @Stable
    data class FeeState(
        override val type: SendUiStateType = SendUiStateType.Fee,
        override val isPrimaryButtonEnabled: Boolean = false,
        val feeSelectorState: FeeSelectorState,
        val fee: Fee?,
        val rate: BigDecimal?,
        val appCurrency: AppCurrency,
        val isFeeApproximate: Boolean,
        val notifications: ImmutableList<SendFeeNotification>,
    ) : SendStates()

    /** Send state */
    @Stable
    data class SendState(
        override val type: SendUiStateType = SendUiStateType.Send,
        override val isPrimaryButtonEnabled: Boolean = true,
        val isSending: Boolean = false,
        val isSuccess: Boolean = false,
        val isSubtract: Boolean = false,
        val transactionDate: Long = 0L,
        val txUrl: String = "",
        val ignoreAmountReduce: Boolean = false,
        val isFromConfirmation: Boolean = true,
        val notifications: ImmutableList<SendNotification> = persistentListOf(),
    ) : SendStates()
}

data class SendUiCurrentScreen(
    val type: SendUiStateType,
    val isFromConfirmation: Boolean,
)

enum class SendUiStateType {
    None,
    Recipient,
    Amount,
    Fee,
    Send,
}