package com.tangem.tap.features.send.redux.reducers

import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.scaleToFiat
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.AmountActionUi.SetMainCurrency
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AmountState
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.send.redux.states.ViewAmountValue
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
class AmountReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = when (action) {
        is AmountActionUi -> handleUiAction(action, sendState, sendState.amountState)
        is AmountAction -> handleAction(action, sendState, sendState.amountState)
        else -> sendState
    }

    private fun handleUiAction(action: AmountActionUi, sendState: SendState, state: AmountState): SendState {
        val result = when (action) {
            is SetMainCurrency -> {
                when (action.mainCurrency) {
                    MainCurrencyType.FIAT -> {
                        val fiatToSend = if (state.amountToSendCrypto.isZero()) BigDecimal.ZERO
                        else sendState.convertToFiat(state.amountToSendCrypto)
                        val rescaledBalance = sendState.convertToFiat(state.balanceCrypto, true)
                        state.copy(
                                viewAmountValue = ViewAmountValue(fiatToSend.stripZeroPlainString()),
                                viewBalanceValue = rescaledBalance.stripZeroPlainString(),
                                mainCurrency = state.createMainCurrency(action.mainCurrency),
                                maxLengthOfAmount = sendState.getDecimals(action.mainCurrency),
                                cursorAtTheSamePosition = false
                        )
                    }
                    MainCurrencyType.CRYPTO -> {
                        state.copy(
                                viewAmountValue = ViewAmountValue(state.amountToSendCrypto.stripZeroPlainString()),
                                viewBalanceValue = state.balanceCrypto.stripZeroPlainString(),
                                mainCurrency = state.createMainCurrency(action.mainCurrency),
                                maxLengthOfAmount = sendState.getDecimals(action.mainCurrency),
                                cursorAtTheSamePosition = false
                        )
                    }
                }
            }
            else -> return sendState
        }

        return updateLastState(sendState.copy(amountState = result), result)
    }

    private fun handleAction(action: AmountAction, sendState: SendState, state: AmountState): SendState {
        val result = when (action) {
            is AmountAction.SetAmount -> {
                when (state.mainCurrency.type) {
                    MainCurrencyType.FIAT -> {
                        val amountCrypto = sendState.convertToCrypto(action.amount)
                        state.copy(
                                viewAmountValue = ViewAmountValue(action.amount.scaleToFiat(false).stripZeroPlainString(), action.isUserInput),
                                amountToSendCrypto = amountCrypto,
                                cursorAtTheSamePosition = true,
                                error = null
                        )
                    }
                    MainCurrencyType.CRYPTO -> {
                        state.copy(
                                viewAmountValue = ViewAmountValue(action.amount.stripZeroPlainString(), action.isUserInput),
                                amountToSendCrypto = action.amount,
                                cursorAtTheSamePosition = true,
                                error = null
                        )
                    }
                }
            }
            is AmountAction.SetAmountError -> {
                state.copy(error = action.error)
            }
        }
        return updateLastState(sendState.copy(amountState = result), result)
    }
}