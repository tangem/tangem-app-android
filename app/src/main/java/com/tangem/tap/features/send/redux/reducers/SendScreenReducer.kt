package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.AmountType
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
                is TransactionExtrasAction -> TransactionExtrasReducer()
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
            is SendAction.Dialog.TezosWarningDialog -> sendState.copy(dialog = action)
            is SendAction.Dialog.SendTransactionFails -> sendState.copy(dialog = action)
            is SendAction.Dialog.Hide -> sendState.copy(dialog = null)
            is SendAction.SetWarnings -> sendState.copy(sendWarningsList = action.warningList)
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
        val prepareAction = action as PrepareSendScreen
        val walletManager = store.state.globalState.scanNoteResponse!!.walletManager!!
        val amountToExtract = prepareAction.tokenAmount ?: prepareAction.coinAmount!!
        val decimals = amountToExtract.decimals

        val coinConverter = createCurrencyConverter(walletManager.wallet.blockchain.currency, decimals)
        val tokenConverter = createCurrencyConverter(prepareAction.tokenAmount?.currencySymbol ?: "", decimals)
        return sendState.copy(
                walletManager = walletManager,
                coinConverter = coinConverter,
                tokenConverter = tokenConverter,
                amountState = sendState.amountState.copy(
                        amountToExtract = amountToExtract,
                        typeOfAmount = amountToExtract.type,
                        balanceCrypto = amountToExtract.value ?: BigDecimal.ZERO
                ),
                feeState = sendState.feeState.copy(
                        includeFeeSwitcherIsEnabled = amountToExtract.type == AmountType.Coin
                )
        )
    }

    private fun createCurrencyConverter(currency: String, decimals: Int): CurrencyConverter? {
        val rate = store.state.globalState.conversionRates.getRate(currency)
        return if (rate == null) null else CurrencyConverter(rate, decimals)
    }
}

internal fun updateLastState(sendState: SendState, lastChangedState: IdStateHolder): SendState {
    sendState.lastChangedStates.add(lastChangedState.stateId)
    return sendState
}
