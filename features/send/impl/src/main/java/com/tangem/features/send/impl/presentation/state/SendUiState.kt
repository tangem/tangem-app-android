package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.event.StateEvent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * Ui states of the send screen
 */
@Immutable
internal data class SendUiState(
    val clickIntents: SendClickIntents,
    val isEditingDisabled: Boolean,
    val cryptoCurrencyName: String,
    val amountState: AmountState,
    val recipientState: SendStates.RecipientState? = null,
    val feeState: SendStates.FeeState? = null,
    val sendState: SendStates.SendState? = null,
    val editAmountState: AmountState,
    val editRecipientState: SendStates.RecipientState? = null,
    val editFeeState: SendStates.FeeState? = null,
    val isBalanceHidden: Boolean,
    val isSubtracted: Boolean,
    val event: StateEvent<SendEvent>,
) {

    fun getAmountState(isEditState: Boolean): AmountState {
        return if (isEditState) {
            editAmountState
        } else {
            amountState
        }
    }

    fun getRecipientState(isEditState: Boolean): SendStates.RecipientState? {
        return if (isEditState) {
            editRecipientState
        } else {
            recipientState
        }
    }

    fun getFeeState(isEditState: Boolean): SendStates.FeeState? {
        return if (isEditState) {
            editFeeState
        } else {
            feeState
        }
    }

    fun copyWrapped(
        isEditState: Boolean,
        amountState: AmountState = this.amountState,
        feeState: SendStates.FeeState? = this.feeState,
        recipientState: SendStates.RecipientState? = this.recipientState,
        sendState: SendStates.SendState? = this.sendState,
    ): SendUiState = if (isEditState) {
        copy(
            editAmountState = amountState,
            editFeeState = feeState,
            editRecipientState = recipientState,
            sendState = sendState,
        )
    } else {
        copy(
            amountState = amountState,
            feeState = feeState,
            recipientState = recipientState,
            sendState = sendState,
        )
    }
}

@Stable
internal sealed class SendStates {

    abstract val type: SendUiStateType

    abstract val isPrimaryButtonEnabled: Boolean

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
        val isFeeConvertibleToFiat: Boolean,
        val appCurrency: AppCurrency,
        val isFeeApproximate: Boolean,
        val isCustomSelected: Boolean,
        val notifications: ImmutableList<NotificationUM>,
        val isTronToken: Boolean,
    ) : SendStates()

    /** Send state */
    @Stable
    data class SendState(
        override val type: SendUiStateType = SendUiStateType.Send,
        override val isPrimaryButtonEnabled: Boolean = false,
        val isSending: Boolean,
        val isSuccess: Boolean,
        val transactionDate: Long,
        val txUrl: String,
        val ignoreAmountReduce: Boolean,
        val reduceAmountBy: BigDecimal?,
        val isFromConfirmation: Boolean,
        val showTapHelp: Boolean,
        val notifications: ImmutableList<NotificationUM>,
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
    EditAmount,
    EditRecipient,
    EditFee,
}