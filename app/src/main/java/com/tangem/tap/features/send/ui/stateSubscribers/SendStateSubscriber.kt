package com.tangem.tap.features.send.ui.stateSubscribers

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.tangem.tap.features.send.redux.FeeLayoutState
import com.tangem.tap.features.send.redux.SendState
import kotlinx.android.synthetic.main.btn_expand_collapse.*
import kotlinx.android.synthetic.main.layout_send_network_fee.*
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class SendStateSubscriber(fragment: Fragment) : FragmentStateSubscriber<SendState>(fragment) {

    override fun updateWithNewState(fg: Fragment, state: SendState) {
        when (state.lastChangedStateType) {
            is FeeLayoutState -> handleFeeLayoutState(fg, state.feeLayoutState)
        }
    }

    private fun handleFeeLayoutState(fg: Fragment, layoutState: FeeLayoutState) {
        Timber.d("handleFeeLayoutState")
        if (fg.llFeeContainer.visibility != layoutState.visibility) {
            val rotationAngle = if (fg.imvExpandCollapse.rotation == 0f) 180f else 0f
            fg.imvExpandCollapse.rotation = rotationAngle

            (fg.llFeeContainer.parent?.parent as? ViewGroup)?.let { TransitionManager.beginDelayedTransition(it) }
            fg.llFeeContainer.visibility = layoutState.visibility
        }

        if (fg.swIncludeFee.isChecked != layoutState.includeFeeIsChecked) {
            fg.swIncludeFee.isChecked = layoutState.includeFeeIsChecked
        }

        if (fg.chipGroup.checkedChipId != layoutState.selectedFeeId) {
            fg.chipGroup.check(layoutState.selectedFeeId)
        }
    }
}