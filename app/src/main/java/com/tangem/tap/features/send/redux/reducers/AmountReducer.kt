package com.tangem.tap.features.send.redux.reducers

import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.AmountActionUi.SetMainCurrency
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AmountState
import com.tangem.tap.features.send.redux.states.InputViewValue
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
            is SetMainCurrency -> {
                val currencyCanBeSwitched = sendState.mainCurrencyCanBeSwitched()

                when (val currency = if (currencyCanBeSwitched) action.mainCurrency else MainCurrencyType.CRYPTO) {
                    MainCurrencyType.FIAT -> {
                        val fiatToSend = if (state.amountToSendCrypto.isZero()) {
                            BigDecimal.ZERO
                        } else {
                            sendState.convertExtractCryptoToFiat(state.amountToSendCrypto)
                        }
                        val rescaledBalance = sendState.convertExtractCryptoToFiat(state.balanceCrypto, true)
                        val viewValue = state.restoreDecimalSeparator(fiatToSend.stripZeroPlainString())
                        state.copy(
                                viewAmountValue = InputViewValue(viewValue),
                                viewBalanceValue = rescaledBalance.stripZeroPlainString(),
                                mainCurrency = state.createMainCurrency(currency, currencyCanBeSwitched),
                                maxLengthOfAmount = sendState.getDecimals(currency),
                                cursorAtTheSamePosition = false
                        )
                    }
                    MainCurrencyType.CRYPTO -> {
                        val viewValue = state.restoreDecimalSeparator(state.amountToSendCrypto.stripZeroPlainString())
                        state.copy(
                                viewAmountValue = InputViewValue(viewValue),
                                viewBalanceValue = state.balanceCrypto.stripZeroPlainString(),
                                mainCurrency = state.createMainCurrency(currency, currencyCanBeSwitched),
                                maxLengthOfAmount = sendState.getDecimals(currency),
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
                val amount = if (state.mainCurrency.type == MainCurrencyType.CRYPTO) {
                    action.amountCrypto
                } else {
                    sendState.convertExtractCryptoToFiat(action.amountCrypto, true)
                }
                val viewValue = state.restoreDecimalSeparator(amount.stripZeroPlainString())
                state.copy(
                        viewAmountValue = InputViewValue(viewValue, action.isUserInput),
                        amountToSendCrypto = action.amountCrypto,
                        cursorAtTheSamePosition = true,
                        error = null
                )
            }
            is AmountAction.SetAmountError -> state.copy(error = action.error)
            is AmountAction.SetDecimalSeparator -> state.copy(decimalSeparator = action.separator)
        }
        return updateLastState(sendState.copy(amountState = result), result)
    }
}