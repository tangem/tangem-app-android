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
        val value: String? = null,
        val payIDWalletAddress: String? = null,
        val error: String? = null,
) : StateType

data class FeeLayoutState(
        val visibility: Int = View.GONE,
        val selectedFeeId: Int = R.id.chipNormal,
        val includeFeeIsChecked: Boolean = false
) : StateType