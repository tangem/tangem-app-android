package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.FeeActionUi
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.FeeState
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.send.redux.states.Value

/**
[REDACTED_AUTHOR]
 */
class FeeReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = when (action) {
        is FeeActionUi -> handleUiAction(action, sendState, sendState.feeState)
        is FeeAction -> handleAction(action, sendState, sendState.feeState)
        else -> sendState
    }

    private fun handleUiAction(action: FeeActionUi, sendState: SendState, state: FeeState): SendState {
        val result = when (action) {
            is FeeActionUi.ToggleControlsVisibility -> state.copy(controlsLayoutIsVisible = !state.controlsLayoutIsVisible)
            is FeeActionUi.ChangeSelectedFee -> {
                val currentFee = createValueOfFeeAmount(action.feeType, state.feeList)
                state.copy(
                        selectedFeeType = action.feeType,
                        currentFee = currentFee
                )

            }
            is FeeActionUi.ChangeIncludeFee -> state.copy(feeIsIncluded = action.isIncluded)
        }
        return updateLastState(sendState.copy(feeState = result), result)
    }

    private fun handleAction(action: FeeAction, sendState: SendState, state: FeeState): SendState {
        val result = when (action) {
            is FeeAction.RequestFee -> {
                state.copy(error = null)
            }
            is FeeAction.ChangeLayoutVisibility -> {
                fun getVisibility(current: Boolean, proposed: Boolean?): Boolean = proposed ?: current
                state.copy(
                        mainLayoutIsVisible = getVisibility(state.mainLayoutIsVisible, action.main),
                        controlsLayoutIsVisible = getVisibility(state.controlsLayoutIsVisible, action.controls),
                        feeChipGroupIsVisible = getVisibility(state.mainLayoutIsVisible, action.chipGroup)
                )
            }
            is FeeAction.FeeCalculation.SetFeeResult -> {
                val fees = action.fee
                if (fees.size == 1) {
                    val feeType = FeeType.SINGLE
                    val currentFee = createValueOfFeeAmount(feeType, fees)

                    state.copy(
                            selectedFeeType = feeType,
                            feeList = fees,
                            currentFee = currentFee,
                            error = null
                    )
                } else {
                    val feeType = getCurrentFeeType(state)
                    val currentFee = createValueOfFeeAmount(feeType, fees)

                    state.copy(
                            selectedFeeType = feeType,
                            feeList = fees,
                            currentFee = currentFee,
                            error = null
                    )
                }
            }
            is FeeAction.FeeCalculation.SetFeeError -> {
                state.copy(
                        feeList = null,
                        currentFee = null,
                        error = action.error
                )
            }
        }

        return updateLastState(sendState.copy(feeState = result), result)
    }

    private fun createValueOfFeeAmount(feeType: FeeType, list: List<Amount>?): Value<Amount>? {
        if (list == null || list.isEmpty()) return null
        val feeAmount = if (list.size == 1) {
            if (!list[0].isAboveZero()) return null

            list[0]
        } else {
            when (feeType) {
                FeeType.SINGLE -> list[1]
                FeeType.LOW -> list[0]
                FeeType.NORMAL -> list[1]
                FeeType.PRIORITY -> list[2]
            }
        }

        return Value(feeAmount, feeAmount.value?.stripZeroPlainString() ?: "")
    }

    private fun getCurrentFeeType(state: FeeState): FeeType {
        return if (state.selectedFeeType == FeeType.SINGLE) FeeType.NORMAL else state.selectedFeeType
    }
}