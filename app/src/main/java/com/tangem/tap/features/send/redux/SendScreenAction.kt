package com.tangem.tap.features.send.redux

import com.tangem.Message
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.ToastNotificationAction
import com.tangem.tap.common.redux.global.StateDialog
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.wallet.R
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface SendScreenAction : Action
interface SendScreenActionUi : SendScreenAction

object ReleaseSendState : Action

data class PrepareSendScreen(
        val coinAmount: Amount?,
        val coinRate: BigDecimal?,
        val walletManager: WalletManager?,
        val tokenAmount: Amount? = null,
        val tokenRate: BigDecimal? = null
) : SendScreenAction

// Address or PayId
sealed class AddressPayIdActionUi : SendScreenActionUi {
    data class HandleUserInput(val data: String) : AddressPayIdActionUi()
    data class PasteAddressPayId(val data: String) : AddressPayIdActionUi()
    data class CheckClipboard(val data: String?) : AddressPayIdActionUi()
    object CheckAddressPayId : AddressPayIdActionUi()
    data class SetTruncateHandler(val handler: (String) -> String) : AddressPayIdActionUi()
    data class TruncateOrRestore(val truncate: Boolean) : AddressPayIdActionUi()
    data class ChangePayIdState(val sendingToPayIdEnabled: Boolean) : AddressPayIdActionUi()
}


sealed class TransactionExtrasAction : SendScreenActionUi {
    data class Prepare(
            val blockchain: Blockchain,
            val walletAddress: String,
            val xrpTag: String?,
    ) : TransactionExtrasAction()

    object Release : TransactionExtrasAction()

    sealed class XlmMemo : TransactionExtrasAction() {
  //        data class ChangeSelectedMemo(val memoType: XlmMemoType) : XlmMemo()
        data class HandleUserInput(val data: String) : XlmMemo()
    }

    sealed class XrpDestinationTag : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : XrpDestinationTag()
    }
}

sealed class AddressPayIdVerifyAction : SendScreenAction {
    enum class Error {
        PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN,
        PAY_ID_NOT_REGISTERED,
        PAY_ID_REQUEST_FAILED,
        ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN,
        ADDRESS_SAME_AS_WALLET
    }

    data class ChangePasteBtnEnableState(val isEnabled: Boolean) : AddressPayIdVerifyAction()

    sealed class PayIdVerification : AddressPayIdVerifyAction() {
        data class SetPayIdError(val error: Error?) : PayIdVerification()
        data class SetPayIdWalletAddress(val payId: String, val payIdWalletAddress: String, val isUserInput: Boolean) : PayIdVerification()
    }

    sealed class AddressVerification : AddressPayIdVerifyAction() {
        data class SetAddressError(val error: Error?) : AddressVerification()
        data class SetWalletAddress(val address: String, val isUserInput: Boolean) : AddressVerification()
    }
}

// Amount to send
sealed class AmountActionUi : SendScreenActionUi {
    data class HandleUserInput(val data: String) : AmountActionUi()
    object CheckAmountToSend : AmountActionUi()
    object SetMaxAmount : AmountActionUi()
    data class SetMainCurrency(val mainCurrency: MainCurrencyType) : AmountActionUi()
    object ToggleMainCurrency : AmountActionUi()
}

sealed class AmountAction : SendScreenAction {
    data class SetAmount(val amountCrypto: BigDecimal, val isUserInput: Boolean) : AmountAction()
    data class SetAmountError(val error: TapError?) : AmountAction()
    data class SetDecimalSeparator(val separator: String) : AmountAction()
}

// Fee
sealed class FeeActionUi : SendScreenActionUi {
    object ToggleControlsVisibility : FeeActionUi()
    data class ChangeSelectedFee(val feeType: FeeType) : FeeActionUi()
    class ChangeIncludeFee(val isIncluded: Boolean) : FeeActionUi()
}

sealed class FeeAction : SendScreenAction {
    enum class Error {
        ADDRESS_OR_AMOUNT_IS_EMPTY,
        REQUEST_FAILED
    }

    object RequestFee : FeeAction()
    sealed class FeeCalculation : FeeAction() {
        data class SetFeeResult(val fee: List<Amount>) : FeeCalculation()
        data class SetFeeError(val error: Error) : FeeCalculation()
    }

    data class ChangeLayoutVisibility(
            val main: Boolean? = null,
            val controls: Boolean? = null,
            val chipGroup: Boolean? = null,
    ) : FeeAction()
}

sealed class ReceiptAction : SendScreenAction {
    object RefreshReceipt : ReceiptAction()
}

sealed class SendActionUi : SendScreenActionUi {
    data class SendAmountToRecipient(val messageForSigner: Message) : SendScreenActionUi
    object CheckIfTransactionDataWasProvided : SendScreenActionUi
}

sealed class SendAction : SendScreenAction {

    data class ChangeSendButtonState(val state: ButtonState) : SendAction()
    object SendSuccess : SendAction(), ToastNotificationAction {
        override val messageResource: Int = R.string.send_transaction_success
    }

    data class SendError(override val error: TapError) : SendAction(), ErrorAction

    sealed class Dialog : SendAction(), StateDialog {
        data class TezosWarningDialog(
                val reduceCallback: () -> Unit,
                val sendAllCallback: () -> Unit,
                val reduceAmount: BigDecimal,
        ) : Dialog()

        data class SendTransactionFails(val errorMessage: String) : Dialog()
        object Hide : Dialog()
    }

    sealed class Warnings : SendAction() {
        object Update : SendAction()
        data class Set(val warningList: List<WarningMessage>) : SendAction()
    }

    data class SendSpecificTransaction(
        val sendAmount: String,
        val destinationAddress: String,
        val transactionId: String
    ) : SendAction()

}