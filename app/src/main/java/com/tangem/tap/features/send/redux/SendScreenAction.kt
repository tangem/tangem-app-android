package com.tangem.tap.features.send.redux

import org.rekotlin.Action

/**
* [REDACTED_AUTHOR]
 */
interface SendScreenAction : Action
interface SendScreenActionUi : SendScreenAction

object ReleaseSendState : Action

// Address or PayId
sealed class AddressPayIdActionUi : SendScreenActionUi {
    data class ChangeAddressOrPayId(val data: String) : AddressPayIdActionUi()
    data class SetTruncateHandler(val handler: (String) -> String) : AddressPayIdActionUi()
    data class TruncateOrRestore(val truncate: Boolean) : AddressPayIdActionUi()
}

sealed class AddressPayIdVerifyAction : SendScreenAction {
    enum class FailReason {
        NONE,
        IS_NOT_PAY_ID,
        PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN,
        PAY_ID_NOT_REGISTERED,
        PAY_ID_REQUEST_FAILED,
        ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN,
        ADDRESS_SAME_AS_WALLET
    }

    sealed class PayIdVerification : AddressPayIdVerifyAction() {
        data class SetError(val payId: String, val reason: FailReason) : PayIdVerification()
        data class SetPayIdWalletAddress(val payId: String, val payIdWalletAddress: String) : PayIdVerification()
    }

    sealed class AddressVerification : AddressPayIdVerifyAction() {
        data class SetError(val address: String, val reason: FailReason) : AddressVerification()
        data class SetWalletAddress(val address: String) : AddressVerification()
    }
}

// Amount to send
sealed class AmountActionUi : SendScreenActionUi {
    object SetMaxAmount : AmountActionUi()
    data class ChangeAmountToSend(val data: String) : AmountActionUi()
    data class SetMainCurrency(val mainCurrency: MainCurrencyType) : AmountActionUi()
    object ToggleMainCurrency : AmountActionUi()
}

// Fee
sealed class FeeActionUi : SendScreenActionUi {
    object ToggleFeeLayoutVisibility : FeeActionUi()
    data class ChangeSelectedFee(val feeType: FeeType) : FeeActionUi()
    class ChangeIncludeFee(val isIncluded: Boolean) : FeeActionUi()
}