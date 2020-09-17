package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.states.IdStateHolder
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.store
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface SendInternalReducer {
    fun handle(action: SendScreenAction, sendState: SendState): SendState
}

class SendScreenReducer {
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
                is SendAction -> SendReducer()
                else -> EmptyReducer()
            }

            return reducer.handle(action, sendState)
        }
    }
}

private class SendReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        val result = when (action) {
            is SendAction.ChangeSendButtonState -> sendState.copy(sendButtonState = action.state)
            else -> return sendState
        }

        return updateLastState(result, result)
    }
}

private class EmptyReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = sendState
}

private class PrepareSendScreenStatesReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        val amount = (action as PrepareSendScreen).tokenAmount ?: action.coinAmount
        val walletManager = store.state.globalState.scanNoteResponse!!.walletManager!!
        return sendState.copy(
                amount = amount,
                walletManager = walletManager,
                currencyConverter = createCurrencyConverter(walletManager),
                amountState = sendState.amountState.copy(balanceCrypto = amount?.value ?: BigDecimal.ZERO)
        )
    }

    private fun createCurrencyConverter(walletManager: WalletManager): CurrencyConverter {
        val rate = store.state.globalState.conversionRates.getRate(walletManager.wallet.blockchain.currency)
        return if (rate == null) CurrencyConverter(BigDecimal.ONE) else CurrencyConverter(rate)
    }
}

internal fun updateLastState(sendState: SendState, lastChangedState: IdStateHolder): SendState {
    sendState.lastChangedStates.add(lastChangedState.stateId)
    return sendState
}
