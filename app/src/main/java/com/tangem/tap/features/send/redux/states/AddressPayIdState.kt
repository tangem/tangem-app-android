package com.tangem.tap.features.send.redux.states

import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction

data class AddressPayIdState(
        val etFieldValue: String? = null,
        val normalFieldValue: String? = null,
        val truncatedFieldValue: String? = null,
        val recipientWalletAddress: String? = null,
        val error: AddressPayIdVerifyAction.Error? = null,
        val truncateHandler: ((String) -> String)? = null,
        val pasteIsEnabled: Boolean = false
) : SendScreenState {

    override val stateId: StateId = StateId.ADDRESS_PAY_ID

    fun isReady(): Boolean = error == null && recipientWalletAddress?.isNotEmpty() ?: false

    fun isPayIdState(): Boolean = recipientWalletAddress != null && recipientWalletAddress != normalFieldValue

    fun copyWalletAddress(address: String): AddressPayIdState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = address,
                normalFieldValue = address,
                truncatedFieldValue = truncated,
                recipientWalletAddress = address,
                error = null
        )
    }

    fun copyError(address: String, error: AddressPayIdVerifyAction.Error): AddressPayIdState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = address,
                normalFieldValue = address,
                truncatedFieldValue = truncated,
                error = error,
                recipientWalletAddress = null
        )
    }

    fun copyPayIdWalletAddress(payId: String, address: String): AddressPayIdState {
        val truncated = truncateHandler?.invoke(payId) ?: address
        return this.copy(
                etFieldValue = payId,
                normalFieldValue = payId,
                truncatedFieldValue = truncated,
                recipientWalletAddress = address,
                error = null
        )
    }

    fun copyPayIdError(payId: String, error: AddressPayIdVerifyAction.Error): AddressPayIdState {
        val truncated = truncateHandler?.invoke(payId) ?: payId
        return this.copy(
                etFieldValue = payId,
                normalFieldValue = payId,
                truncatedFieldValue = truncated,
                error = error,
                recipientWalletAddress = null
        )
    }
}