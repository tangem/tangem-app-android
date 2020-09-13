package com.tangem.tap.features.send.redux

import com.tangem.blockchain.common.Amount
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface SendScreenAction : Action
interface SendScreenActionUi : SendScreenAction

object ReleaseSendState : Action

data class PrepareSendScreen(
        val amount: Amount,
) : SendScreenAction

// Address or PayId
sealed class AddressPayIdActionUi : SendScreenActionUi {
    data class ChangeAddressOrPayId(val data: String) : AddressPayIdActionUi()
    data class SetTruncateHandler(val handler: (String) -> String) : AddressPayIdActionUi()
    data class TruncateOrRestore(val truncate: Boolean) : AddressPayIdActionUi()
}

sealed class AddressPayIdVerifyAction : SendScreenAction {
    enum class Error {
        IS_NOT_PAY_ID,
        PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN,
        PAY_ID_NOT_REGISTERED,
        PAY_ID_REQUEST_FAILED,
        ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN,
        ADDRESS_SAME_AS_WALLET
    }

    sealed class PayIdVerification : AddressPayIdVerifyAction() {
        data class SetError(val payId: String, val error: Error) : PayIdVerification()
        data class SetPayIdWalletAddress(val payId: String, val payIdWalletAddress: String) : PayIdVerification()
    }

    sealed class AddressVerification : AddressPayIdVerifyAction() {
        data class SetError(val address: String, val error: Error) : AddressVerification()
        data class SetWalletAddress(val address: String) : AddressVerification()
    }
}

// Amount to send
sealed class AmountActionUi : SendScreenActionUi {
    object SetMaxAmount : AmountActionUi()
    data class CheckAmountToSend(val data: String? = null) : AmountActionUi()
    data class SetMainCurrency(val mainCurrency: MainCurrencyType) : AmountActionUi()
    object ToggleMainCurrency : AmountActionUi()
}

sealed class AmountAction : SendScreenAction {
    enum class Error {
        FEE_GREATER_THAN_AMOUNT,
        AMOUNT_WITH_FEE_GREATER_THAN_BALANCE
    }

    sealed class AmountVerification : AmountAction() {
        data class SetAmount(val amount: BigDecimal) : AmountVerification()
        data class SetError(val amount: BigDecimal, val error: Error) : AmountVerification()
    }
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