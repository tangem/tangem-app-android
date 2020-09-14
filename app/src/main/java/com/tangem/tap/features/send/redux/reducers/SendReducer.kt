package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.states.IdStateHolder
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.store
import org.rekotlin.Action
import timber.log.Timber
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface SendInternalReducer {
    fun handle(action: SendScreenAction, sendState: SendState): SendState
}

class SendReducer {
    companion object {
        fun reduce(incomingAction: Action, sendState: SendState): SendState {
            if (incomingAction is ReleaseSendState) return SendState()
            val action = incomingAction as? SendScreenAction ?: return sendState

            val reducer: SendInternalReducer = when (action) {
                is PrepareSendScreen -> PrepareSendScreenStatesReducer()
                is AddressPayIdActionUi, is AddressPayIdVerifyAction -> AddressPayIdReducer()
                is AmountActionUi, is AmountAction -> AmountReducer()
                is FeeActionUi, is FeeAction -> FeeReducer()
                is ReceiptAction -> ReceiptReducer()
                else -> EmptyReducer()
            }


            val newState = reducer.handle(action, sendState).copy(sendButtonIsEnabled = sendState.isReadyToSend())
            Timber.i("${newState.lastChangedStates}.")

            return newState
        }
    }
}

private class EmptyReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = sendState
}

private class PrepareSendScreenStatesReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        val amount = (action as PrepareSendScreen).amount
        val walletManager = store.state.globalState.scanNoteResponse!!.walletManager!!
        return sendState.copy(
                amount = amount,
                walletManager = walletManager,
                currencyConverter = createCurrencyConverter(walletManager),
                amountState = sendState.amountState.copy(balanceCrypto = amount.value ?: BigDecimal.ZERO)
        )
    }

    private fun createCurrencyConverter(walletManager: WalletManager): CurrencyConverter {
        val rate = store.state.globalState.fiatRates.getRateForCryptoCurrency(walletManager.wallet.blockchain.currency)
        return if (rate == null) CurrencyConverter(BigDecimal.ONE) else CurrencyConverter(rate)
    }
}

internal fun updateLastState(sendState: SendState, lastChangedState: IdStateHolder): SendState {
    sendState.lastChangedStates.add(lastChangedState.stateId)
    return sendState
}
