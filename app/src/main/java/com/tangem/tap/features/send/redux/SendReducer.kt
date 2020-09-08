package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.blockchain.common.AmountType
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.*
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification
import com.tangem.tap.features.send.redux.AmountActionUi.ChangeAmountToSend
import com.tangem.tap.features.send.redux.AmountActionUi.SetMainCurrency
import com.tangem.tap.features.send.redux.FeeActionUi.*
import com.tangem.tap.store
import org.rekotlin.Action
import org.rekotlin.StateType
import timber.log.Timber
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class SendReducer {
    companion object {
        fun reduce(action: Action, sendState: SendState): SendState {
            val newState = internalReduce(action, sendState)
            if (newState == sendState) Timber.i("state didn't modified.")
            else Timber.i("state was updated to: $newState.")
            return newState
        }
    }
}

private fun internalReduce(incomingAction: Action, sendState: SendState): SendState {
    if (incomingAction is ReleaseSendState) return SendState()
    val action = incomingAction as? SendScreenAction ?: return sendState

    var state = when (action) {
        is AddressPayIdActionUi -> handleAddressPayIdActionUi(action, sendState, sendState.addressPayIdState)
        is AddressPayIdVerifyAction -> handleAddressPayIdAction(action, sendState, sendState.addressPayIdState)
        is AmountActionUi -> handleAmountActionUi(action, sendState, sendState.amountState)
        is FeeActionUi -> handleFeeActionUi(action, sendState, sendState.feeLayoutState)
        else -> sendState
    }
    state = state.copy(sendButtonIsEnabled = state.addressPayIdState.error == null
            && state.addressPayIdState.walletAddress?.isNotEmpty() ?: false
            && !state.amountState.amountIsOverBalance
            && !state.amountState.amountToSendCrypto.isZero()
    )
    return state
}

fun handleAddressPayIdActionUi(
        action: AddressPayIdActionUi,
        sendState: SendState,
        state: AddressPayIdState
): SendState {
    val result = when (action) {
        is ChangeAddressOrPayId -> state
        is SetTruncateHandler -> state.copy(truncateHandler = action.handler)
        is TruncateOrRestore -> {
            if (action.truncate) state.copy(etFieldValue = state.truncatedFieldValue)
            else state.copy(etFieldValue = state.normalFieldValue)
        }
    }
    return updateLastState(sendState.copy(addressPayIdState = result), result)
}

private fun handleAddressPayIdAction(
        action: AddressPayIdVerifyAction,
        sendState: SendState,
        state: AddressPayIdState
): SendState {
    val result = when (action) {
        is PayIdVerification.SetPayIdWalletAddress -> state.copyPayIdWalletAddress(action.payId, action.payIdWalletAddress)
        is PayIdVerification.SetError -> state.copyPaiIdError(action.payId, action.reason)
        is AddressVerification.SetWalletAddress -> state.copyWalletAddress(action.address)
        is AddressVerification.SetError -> state.copyError(action.address, action.reason)
    }
    return updateLastState(sendState.copy(addressPayIdState = result), result)
}

private fun handleAmountActionUi(action: AmountActionUi, sendState: SendState, state: AmountState): SendState {
    val rates = store.state.globalState.fiatRates
    val wallet = store.state.walletState.wallet ?: return sendState
    val fiatRate = rates.getRateForCryptoCurrency(wallet.blockchain.currency) ?: return sendState

    val walletAmount = wallet.amounts[AmountType.Token] ?: wallet.amounts[AmountType.Coin]
    val converter = CurrencyConverter(fiatRate)

    val result = when (action) {
        is SetMainCurrency -> {
            when (action.mainCurrency) {
                MainCurrencyType.FIAT -> {
                    val fiatToSend = if (state.amountToSendCrypto.isZero()) BigDecimal.ZERO
                    else converter.toFiat(state.amountToSendCrypto)

                    val fiatBalance = converter.toFiat(walletAmount?.value ?: BigDecimal.ZERO)

                    state.copy(
                            etAmountFieldValue = fiatToSend.stripZeroPlainString(),
                            balance = fiatBalance,
                            mainCurrency = Value(MainCurrencyType.FIAT, TapCurrency.main),
                            cursorAtTheSamePosition = false
                    )
                }
                MainCurrencyType.CRYPTO -> {
                    val cryptoBalance = walletAmount?.value ?: BigDecimal.ZERO
                    state.copy(
                            etAmountFieldValue = state.amountToSendCrypto.stripZeroPlainString(),
                            amountToSendCrypto = state.amountToSendCrypto,
                            balance = cryptoBalance.stripTrailingZeros(),
                            mainCurrency = Value(MainCurrencyType.CRYPTO, walletAmount?.currencySymbol ?: "null"),
                            cursorAtTheSamePosition = false
                    )
                }
            }
        }
        is AmountActionUi.SetMaxAmount -> {
            when (state.mainCurrency.value) {
                MainCurrencyType.FIAT -> {
                    val cryptoBalance = walletAmount?.value ?: BigDecimal.ZERO
                    val fiatToSend = converter.toFiat(cryptoBalance)

                    state.copy(
                            etAmountFieldValue = fiatToSend.stripZeroPlainString(),
                            amountToSendCrypto = cryptoBalance,
                            cursorAtTheSamePosition = false
                    )
                }
                MainCurrencyType.CRYPTO -> {
                    val cryptoBalance = walletAmount?.value ?: BigDecimal.ZERO
                    state.copy(
                            etAmountFieldValue = cryptoBalance.stripZeroPlainString(),
                            amountToSendCrypto = cryptoBalance,
                            cursorAtTheSamePosition = false
                    )
                }
            }

        }
        is ChangeAmountToSend -> {
            val bdData = when {
                action.data.isEmpty() || action.data == "0" -> BigDecimal.ZERO
                else -> BigDecimal(action.data)
            }

            when (state.mainCurrency.value) {
                MainCurrencyType.FIAT -> {
                    val sendCrypto = converter.toCrypto(bdData, wallet.blockchain.decimals()).stripTrailingZeros()
                    state.copy(
                            etAmountFieldValue = action.data,
                            amountToSendCrypto = sendCrypto,
                            amountIsOverBalance = state.balance < bdData,
                            cursorAtTheSamePosition = true
                    )
                }
                MainCurrencyType.CRYPTO -> {
                    state.copy(
                            etAmountFieldValue = action.data,
                            amountToSendCrypto = bdData,
                            amountIsOverBalance = state.balance < bdData,
                            cursorAtTheSamePosition = true
                    )
                }
            }
        }
        else -> state
    }
    return updateLastState(sendState.copy(amountState = result), result)
}

private fun handleFeeActionUi(action: FeeActionUi, sendState: SendState, state: FeeLayoutState): SendState {
    val result = when (action) {
        is ToggleFeeLayoutVisibility -> {
            state.copy(visibility = if (state.visibility == View.VISIBLE) View.GONE else View.VISIBLE)
        }
        is ChangeSelectedFee -> state.copy(feeType = action.feeType)
        is ChangeIncludeFee -> state.copy(feeIsIncluded = action.isIncluded)
    }
    return updateLastState(sendState.copy(feeLayoutState = result), result)
}

private fun updateLastState(sendState: SendState, lastChangedState: StateType): SendState =
        sendState.copy(lastChangedStateType = lastChangedState)