package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.blockchain.common.WalletManager
import com.tangem.wallet.R
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class SendState(
        val walletManager: WalletManager? = null,
        val lastChangedStateType: StateType = NoneState(),
        val addressPayIDState: AddressPayIDState = AddressPayIDState(),
        val feeLayoutState: FeeLayoutState = FeeLayoutState()
) : StateType

class NoneState : StateType

data class AddressPayIDState(
        val etFieldValue: String? = null,
        val normalFieldValue: String? = null,
        val truncatedFieldValue: String? = null,
        val walletAddress: String? = null,
        val error: AddressPayIdVerifyAction.FailReason? = null,
        val truncateHandler: ((String) -> String)? = null
) : StateType {

    fun isPayIdState(): Boolean = walletAddress != null && walletAddress != normalFieldValue

    fun copyWalletAddress(address: String): AddressPayIDState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = address,
                normalFieldValue = address,
                truncatedFieldValue = truncated,
                walletAddress = address,
                error = null
        )
    }

    fun copyError(address: String, error: AddressPayIdVerifyAction.FailReason): AddressPayIDState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = address,
                normalFieldValue = address,
                truncatedFieldValue = truncated,
                error = error,
                walletAddress = null
        )
    }

    fun copyPayIdWalletAddress(payId: String, address: String): AddressPayIDState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = payId,
                normalFieldValue = payId,
                truncatedFieldValue = truncated,
                walletAddress = address,
                error = null
        )
    }

    fun copyPaiIdError(payId: String, error: AddressPayIdVerifyAction.FailReason): AddressPayIDState {
        val truncated = truncateHandler?.invoke(payId) ?: payId
        return this.copy(
                etFieldValue = payId,
                normalFieldValue = payId,
                truncatedFieldValue = truncated,
                error = error,
                walletAddress = null
        )
    }
}

data class FeeLayoutState(
        val visibility: Int = View.GONE,
        val selectedFeeId: Int = R.id.chipNormal,
        val includeFeeIsChecked: Boolean = false
) : StateType