package com.tangem.tap.features.send.redux.states

import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction

data class AddressPayIdState(
        val viewFieldValue: InputViewValue = InputViewValue(""),
        val normalFieldValue: String? = null,
        val truncatedFieldValue: String? = null,
        val destinationWalletAddress: String? = null,
        val error: AddressPayIdVerifyAction.Error? = null,
        val truncateHandler: ((String) -> String)? = null,
        val walletPayIdEnabled: Boolean = false,
        val pasteIsEnabled: Boolean = false
) : SendScreenState {

    override val stateId: StateId = StateId.ADDRESS_PAY_ID

    fun truncate(value: String): String = truncateHandler?.invoke(value) ?: value

    fun isReady(): Boolean = error == null && destinationWalletAddress?.isNotEmpty() ?: false

    fun isPayIdState(): Boolean = destinationWalletAddress != null && destinationWalletAddress != normalFieldValue
}