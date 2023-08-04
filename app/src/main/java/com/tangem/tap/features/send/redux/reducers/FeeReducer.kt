package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.FeeActionUi
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.FeeState
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.wallet.redux.ProgressState

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
            is FeeActionUi.ToggleControlsVisibility -> {
                state.copy(controlsLayoutIsVisible = !state.controlsLayoutIsVisible)
            }
            is FeeActionUi.ChangeSelectedFee -> {
                val currentFee = state.fees?.let {
                    createValueOfFeeAmount(action.feeType, it)
                }
                state.copy(
                    selectedFeeType = action.feeType,
                    currentFee = currentFee,
                )
            }
            is FeeActionUi.ChangeIncludeFee -> state.copy(feeIsIncluded = action.isIncluded)
        }
        return updateLastState(sendState.copy(feeState = result), result)
    }

    private fun handleAction(action: FeeAction, sendState: SendState, state: FeeState): SendState {
        val result = when (action) {
            is FeeAction.RequestFee -> {
                state.copy(progressState = ProgressState.Loading)
            }
            is FeeAction.ChangeLayoutVisibility -> {
                fun getVisibility(current: Boolean, proposed: Boolean?): Boolean = proposed ?: current
                state.copy(
                    mainLayoutIsVisible = getVisibility(state.mainLayoutIsVisible, action.main),
                    controlsLayoutIsVisible = getVisibility(state.controlsLayoutIsVisible, action.controls),
                    feeChipGroupIsVisible = getVisibility(state.feeChipGroupIsVisible, action.chipGroup),
                )
            }
            is FeeAction.FeeCalculation.SetFeeResult -> {
                when (val fees = action.fee) {
                    is TransactionFee.Single -> {
                        val feeType = FeeType.SINGLE
                        val currentFee = createValueOfFeeAmount(feeType, fees)

                        state.copy(
                            selectedFeeType = feeType,
                            fees = fees,
                            currentFee = currentFee,
                            feeIsApproximate = isFeeApproximate(sendState),
                        )
                    }
                    is TransactionFee.Choosable -> {
                        val feeType = getCurrentFeeType(state)
                        val currentFee = createValueOfFeeAmount(feeType, fees)

                        state.copy(
                            selectedFeeType = feeType,
                            fees = fees,
                            currentFee = currentFee,
                            feeIsApproximate = isFeeApproximate(sendState),
                        )
                    }
                }.copy(
                    progressState = ProgressState.Done,
                )
            }
            FeeAction.FeeCalculation.ClearResult -> {
                state.copy(
                    fees = null,
                    currentFee = null,
                    progressState = ProgressState.Done,
                )
            }
        }

        return updateLastState(sendState.copy(feeState = result), result)
    }

    private fun createValueOfFeeAmount(feeType: FeeType, transactionFee: TransactionFee): Fee {
        return when (transactionFee) {
            is TransactionFee.Single -> {
                transactionFee.normal
            }
            is TransactionFee.Choosable -> {
                when (feeType) {
                    FeeType.SINGLE -> transactionFee.normal
                    FeeType.LOW -> transactionFee.minimum
                    FeeType.NORMAL -> transactionFee.normal
                    FeeType.PRIORITY -> transactionFee.priority
                }
            }
        }
    }

    private fun isFeeApproximate(sendState: SendState): Boolean {
        val blockchain = sendState.walletManager?.wallet?.blockchain ?: return false

        val amountType = sendState.amountState.typeOfAmount
        return blockchain.isFeeApproximate(amountType)
    }

    private fun getCurrentFeeType(state: FeeState): FeeType {
        return if (state.selectedFeeType == FeeType.SINGLE) FeeType.NORMAL else state.selectedFeeType
    }
}