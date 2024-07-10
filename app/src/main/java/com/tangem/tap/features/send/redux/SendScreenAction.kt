package com.tangem.tap.features.send.redux

import com.tangem.Message
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.common.analytics.events.Token.Send.AddressEntered
import com.tangem.tap.common.redux.ToastNotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.wallet.R
import org.rekotlin.Action
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
interface SendScreenAction : Action
interface SendScreenActionUi : SendScreenAction

object ReleaseSendState : Action

data class PrepareSendScreen(
    val walletManager: WalletManager,
    val feePaidCurrency: FeePaidCurrency,
    val currency: CryptoCurrency,
    val coinAmount: Amount?,
    val coinRate: BigDecimal?,
    val tokenAmount: Amount? = null,
    val tokenRate: BigDecimal? = null,
    val feeCurrencyRate: BigDecimal? = null,
    val feeCurrencyDecimals: Int = 0,
) : SendScreenAction

// Address
sealed class AddressActionUi : SendScreenActionUi {
    data class HandleUserInput(val data: String) : AddressActionUi()
    data class PasteAddress(val data: String, val sourceType: AddressEntered.SourceType) : AddressActionUi()
    data class CheckClipboard(val data: String?) : AddressActionUi()
    data class CheckAddress(val sourceType: AddressEntered.SourceType?) : AddressActionUi()
    data class SetTruncateHandler(val handler: (String) -> String) : AddressActionUi()
    data class TruncateOrRestore(val truncate: Boolean) : AddressActionUi()
}

sealed class TransactionExtrasAction : SendScreenActionUi {
    data class Prepare(
        val blockchain: Blockchain,
        val walletAddress: String,
        val xrpTag: String?,
    ) : TransactionExtrasAction()

    object Release : TransactionExtrasAction()

    @Deprecated("Only in legacy send screen")
    sealed class XlmMemo : TransactionExtrasAction() {
        //        data class ChangeSelectedMemo(val memoType: XlmMemoType) : XlmMemo()
        data class HandleUserInput(val data: String) : XlmMemo()
    }

    @Deprecated("Only in legacy send screen")
    sealed class BinanceMemo : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : BinanceMemo()
    }

    @Deprecated("Only in legacy send screen")
    sealed class XrpDestinationTag : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : XrpDestinationTag()
    }

    @Deprecated("Only in legacy send screen")
    sealed class TonMemo : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : TonMemo()
    }

    @Deprecated("Only in legacy send screen")
    sealed class CosmosMemo : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : CosmosMemo()
    }

    @Deprecated("Only in legacy send screen")
    sealed class HederaMemo : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : HederaMemo()
    }

    @Deprecated("Only in legacy send screen")
    sealed class AlgorandMemo : TransactionExtrasAction() {
        data class HandleUserInput(val data: String) : AlgorandMemo()
    }
}

sealed class AddressVerifyAction : SendScreenAction {
    enum class Error {
        ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN,
        ADDRESS_SAME_AS_WALLET,
    }

    data class ChangePasteBtnEnableState(val isEnabled: Boolean) : AddressVerifyAction()

    sealed class AddressVerification : AddressVerifyAction() {
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
    data class HideBalance(val hide: Boolean) : AmountAction()
}

// Fee
sealed class FeeActionUi : SendScreenActionUi {
    object ToggleControlsVisibility : FeeActionUi()
    data class ChangeSelectedFee(val feeType: FeeType) : FeeActionUi()
    class ChangeIncludeFee(val isIncluded: Boolean) : FeeActionUi()
}

sealed class FeeAction : SendScreenAction {

    object RequestFee : FeeAction()
    sealed class FeeCalculation : FeeAction() {
        data class SetFeeResult(val fee: TransactionFee) : FeeCalculation()
        object ClearResult : FeeCalculation()
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

    sealed class Dialog : SendAction(), StateDialog {
        data class TezosWarningDialog(
            val reduceCallback: () -> Unit,
            val sendAllCallback: () -> Unit,
            val reduceAmount: BigDecimal,
        ) : Dialog()

        data class KaspaWarningDialog(
            val maxOutputs: Int,
            val maxAmount: BigDecimal,
            val onOk: () -> Unit,
        ) : Dialog()

        data class ChiaWarningDialog(
            val blockchainName: String,
            val maxOutputs: Int,
            val maxAmount: BigDecimal,
            val onOk: () -> Unit,
        ) : Dialog()

        sealed class SendTransactionFails : Dialog() {
            data class CardSdkError(val error: TangemSdkError, val scanResponse: ScanResponse) : Dialog()
            data class BlockchainSdkError(
                val error: com.tangem.blockchain.common.BlockchainSdkError,
                val scanResponse: ScanResponse,
            ) : Dialog()
        }

        data class RequestFeeError(
            val error: com.tangem.blockchain.common.BlockchainSdkError,
            val scanResponse: ScanResponse,
            val onRetry: () -> Unit,
        ) : Dialog()

        object Hide : Dialog()
    }

    sealed class Warnings : SendAction() {
        object Update : SendAction()
        data class Set(val warningList: List<WarningMessage>) : SendAction()
    }

    data class SendSpecificTransaction(
        val sendAmount: String,
        val destinationAddress: String,
        val transactionId: String,
    ) : SendAction()
}
