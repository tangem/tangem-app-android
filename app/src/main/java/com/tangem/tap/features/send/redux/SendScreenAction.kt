package com.tangem.tap.features.send.redux

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
interface SendScreenAction : Action
interface SendScreenActionUI : SendScreenAction

object ReleaseSendState : Action

sealed class FeeActionUI : SendScreenActionUI {
    object ToggleFeeLayoutVisibility : FeeActionUI()
    data class ChangeSelectedFee(val id: Int) : FeeActionUI()
    class ChangeIncludeFee(val isChecked: Boolean) : FeeActionUI()
}

// shortness AddressOrPayId = APid
sealed class AddressPayIdActionUI : SendScreenActionUI {
    data class SetAddressOrPayId(val data: String) : AddressPayIdActionUI()
    data class SetTruncateHandler(val handler: (String) -> String) : AddressPayIdActionUI()
    data class TruncateOrRestore(val truncate: Boolean) : AddressPayIdActionUI()
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
        data class Failed(val payId: String, val reason: FailReason) : PayIdVerification()
        data class Success(val payId: String, val payIdWalletAddress: String) : PayIdVerification()
    }

    sealed class AddressVerification : AddressPayIdVerifyAction() {
        data class Failed(val address: String, val reason: FailReason) : AddressVerification()
        data class Success(val address: String) : AddressVerification()
    }
}