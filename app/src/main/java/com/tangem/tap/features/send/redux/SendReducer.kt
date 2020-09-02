package com.tangem.tap.features.send.redux

import android.view.View
import org.rekotlin.Action
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
class SendReducer {
    companion object {
        fun reduce(action: Action, state: SendState): SendState {
            val sendAction = action as? SendAction ?: return state

            return when (sendAction) {
                is FeeLayout -> handleFeeLayoutAction(sendAction, state, state.feeLayoutState)
                else -> state
            }
        }

        private fun handleFeeLayoutAction(action: FeeLayout, sendState: SendState, state: FeeLayoutState): SendState {
            return when (action) {
                is FeeLayout.ToggleFeeLayoutVisibility -> {
                    val visibility = if (state.visibility == View.VISIBLE) View.GONE
                    else View.VISIBLE

                    val result = state.copy(visibility = visibility)
                    updateLastState(sendState.copy(feeLayoutState = result), result)
                }
                is FeeLayout.ChangeSelectedFee -> {
                    val result = state.copy(selectedFeeId = action.id)
                    updateLastState(sendState.copy(feeLayoutState = result), result)
                }
                is FeeLayout.ChangeIncludeFee -> {
                    val result = state.copy(includeFeeIsChecked = action.isChecked)
                    updateLastState(sendState.copy(feeLayoutState = result), result)
                }
            }
        }

        private fun updateLastState(sendState: SendState, state: StateType): SendState {
            return sendState.copy(lastChangedStateType = state)
        }
    }
}
