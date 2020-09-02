package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.wallet.R
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class SendState(
        val lastChangedStateType: StateType = NoneState(),
        val feeLayoutState: FeeLayoutState = FeeLayoutState()
) : StateType

class NoneState : StateType

data class FeeLayoutState(
        val visibility: Int = View.GONE,
        val selectedFeeId: Int = R.id.chipNormal,
        val includeFeeIsChecked: Boolean = false
) : StateType