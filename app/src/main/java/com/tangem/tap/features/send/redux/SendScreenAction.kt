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

sealed class AddressPayIdActionUI : SendScreenActionUI {
    data class SetAddressOrPayId(val data: CharSequence?) : AddressPayIdActionUI()
}

sealed class AddressPayIdAction : SendScreenAction {
    object Verification : AddressPayIdAction() {
        object PayIdNotSupportedByBlockchain : AddressPayIdAction()
        object Failed : AddressPayIdAction()
        data class Success(val payIdWalletAddress: String) : AddressPayIdAction()
    }
}