package com.tangem.tap.features.send.redux.reducers

import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.isNegative
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.AmountActionUi.*
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AmountState
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.SendState
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class AmountReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = when (action) {
        is AmountActionUi -> handleUiAction(action, sendState, sendState.amountState)
        is AmountAction -> handleAction(action, sendState, sendState.amountState)
        else -> sendState
    }

    private fun handleUiAction(action: AmountActionUi, sendState: SendState, state: AmountState): SendState {
        val result = when (action) {
            is ToggleMainCurrency -> {
                val type = if (state.mainCurrency.value == MainCurrencyType.FIAT) MainCurrencyType.CRYPTO
                else MainCurrencyType.FIAT

                return handleUiAction(SetMainCurrency(type), sendState, state)
            }
            is SetMainCurrency -> {
                when (action.mainCurrency) {
                    MainCurrencyType.FIAT -> {
                        val fiatToSend = if (state.amountToSendCrypto.isZero()) BigDecimal.ZERO
                        else sendState.convertToFiat(state.amountToSendCrypto)
                        val rescaledBalance = sendState.convertToFiat(state.balanceCrypto, true)
                        state.copy(
                                viewAmountValue = fiatToSend.stripZeroPlainString(),
                                viewBalanceValue = rescaledBalance.stripZeroPlainString(),
                                mainCurrency = state.createMainCurrencyValue(action.mainCurrency),
                                maxLengthOfAmount = sendState.getDecimals(action.mainCurrency),
                                cursorAtTheSamePosition = false
                        )
                    }
                    MainCurrencyType.CRYPTO -> {
                        state.copy(
                                viewAmountValue = state.amountToSendCrypto.stripZeroPlainString(),
                                viewBalanceValue = state.balanceCrypto.stripZeroPlainString(),
                                mainCurrency = state.createMainCurrencyValue(action.mainCurrency),
                                maxLengthOfAmount = sendState.getDecimals(action.mainCurrency),
                                cursorAtTheSamePosition = false
                        )
                    }
                }
            }
            is SetMaxAmount -> {
                val maxAmount = if (sendState.feeState.feeIsIncluded) {
                    state.balanceCrypto
                } else {
                    val balanceExtractFee = state.balanceCrypto.minus(sendState.feeState.getCurrentFee())
                    if (balanceExtractFee.isNegative()) BigDecimal.ZERO
                    else balanceExtractFee
                }

                val etFieldValue = if (state.mainCurrency.value == MainCurrencyType.CRYPTO) maxAmount
                else sendState.convertToFiat(maxAmount)
                state.copy(
                        viewAmountValue = etFieldValue.stripZeroPlainString(),
                        amountToSendCrypto = maxAmount,
                        cursorAtTheSamePosition = false
                )
            }
            is CheckAmountToSend -> return sendState
        }

        return updateLastState(sendState.copy(amountState = result), result)
    }

    private fun handleAction(action: AmountAction, sendState: SendState, state: AmountState): SendState {
        val result = when (action) {
            is AmountAction.AmountVerification.SetAmount -> {
                setAmount(sendState, action.amount, state)
            }
            is AmountAction.AmountVerification.SetError -> {
                setAmount(sendState, action.amount, state).copy(error = action.error)
            }
        }
        return updateLastState(sendState.copy(amountState = result), result)
    }

    private fun setAmount(sendState: SendState, amount: BigDecimal, state: AmountState): AmountState {
        return when (state.mainCurrency.value) {
            MainCurrencyType.FIAT -> {
                val amountCrypto = sendState.convertToCrypto(amount)
                state.copy(
                        viewAmountValue = amount.stripZeroPlainString(),
                        amountToSendCrypto = amountCrypto,
                        cursorAtTheSamePosition = true,
                        error = null
                )
            }
            MainCurrencyType.CRYPTO -> {
                state.copy(
                        viewAmountValue = amount.stripZeroPlainString(),
                        amountToSendCrypto = amount,
                        cursorAtTheSamePosition = true,
                        error = null
                )
            }
        }
    }
}