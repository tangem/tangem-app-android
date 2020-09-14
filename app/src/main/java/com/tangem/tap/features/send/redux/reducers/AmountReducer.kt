package com.tangem.tap.features.send.redux.reducers

import com.tangem.common.extensions.isZero
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.common.extensions.isNegative
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.AmountActionUi.*
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AmountState
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.send.redux.states.Value
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
        val converter = sendState.currencyConverter
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
                        else converter.toFiat(state.amountToSendCrypto)
                        state.copy(
                                viewAmountValue = fiatToSend.stripZeroPlainString(),
                                viewBalanceValue = converter.toFiat(state.balanceCrypto).stripZeroPlainString(),
                                mainCurrency = Value(MainCurrencyType.FIAT, TapCurrency.main),
                                cursorAtTheSamePosition = false
                        )
                    }
                    MainCurrencyType.CRYPTO -> {
                        val mainCurrency = Value(MainCurrencyType.CRYPTO, sendState.amount?.currencySymbol ?: "null")
                        state.copy(
                                viewAmountValue = state.amountToSendCrypto.stripZeroPlainString(),
                                viewBalanceValue = state.balanceCrypto.stripZeroPlainString(),
                                mainCurrency = mainCurrency,
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
                else converter.toFiat(maxAmount)
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
        val decimals = sendState.amount?.decimals ?: return sendState

        val result = when (action) {
            is AmountAction.AmountVerification.SetAmount -> {
                setAmount(sendState.currencyConverter, decimals, action.amount, state)
            }
            is AmountAction.AmountVerification.SetError -> {
                setAmount(sendState.currencyConverter, decimals, action.amount, state).copy(error = action.error)
            }
        }

        return updateLastState(sendState.copy(amountState = result), result)

    }

    private fun setAmount(converter: CurrencyConverter, decimals: Int, amount: BigDecimal, state: AmountState): AmountState {
        return when (state.mainCurrency.value) {
            MainCurrencyType.FIAT -> {
                val amountCrypto = converter.toCrypto(amount, decimals).stripTrailingZeros()
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