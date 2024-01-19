package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.states.ExternalTransactionData
import com.tangem.tap.features.send.redux.states.IdStateHolder
import com.tangem.tap.features.send.redux.states.SendState
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface SendInternalReducer {
    fun handle(action: SendScreenAction, sendState: SendState): SendState
}

object SendScreenReducer {
    fun reduce(incomingAction: Action, sendState: SendState): SendState {
        if (incomingAction is ReleaseSendState) return SendState()
        val action = incomingAction as? SendScreenAction ?: return sendState

        val reducer: SendInternalReducer = when (action) {
            is PrepareSendScreen -> PrepareSendScreenStatesReducer()
            is AddressActionUi, is AddressVerifyAction -> AddressReducer()
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

private class SendReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        val result = when (action) {
            is SendAction.ChangeSendButtonState -> {
                sendState.copy(sendButtonState = IndeterminateProgressButton(action.state))
            }
            is SendAction.Dialog.TezosWarningDialog -> sendState.copy(dialog = action)
            is SendAction.Dialog.KaspaWarningDialog -> sendState.copy(dialog = action)
            is SendAction.Dialog.ChiaWarningDialog -> sendState.copy(dialog = action)
            is SendAction.Dialog.SendTransactionFails.CardSdkError -> sendState.copy(dialog = action)
            is SendAction.Dialog.SendTransactionFails.BlockchainSdkError -> sendState.copy(dialog = action)
            is SendAction.Dialog.RequestFeeError -> sendState.copy(dialog = action)
            is SendAction.Dialog.Hide -> sendState.copy(dialog = null)
            is SendAction.Warnings.Set -> sendState.copy(sendWarningsList = action.warningList)
            is SendAction.SendSpecificTransaction -> handleSendSpecificTransactionAction(action, sendState)
            is SendAction.SendSuccess -> sendState.copy(isSuccessSend = true)
            else -> return sendState
        }

        return updateLastState(result, result)
    }

    private fun handleSendSpecificTransactionAction(
        action: SendAction.SendSpecificTransaction,
        state: SendState,
    ): SendState {
        return state.copy(
            externalTransactionData =
            ExternalTransactionData(action.sendAmount, action.destinationAddress, action.transactionId),
            feeState = state.feeState.copy(
                includeFeeSwitcherIsEnabled = false,
            ),
            amountState = state.amountState.copy(
                inputIsEnabled = false,
            ),
            addressState = state.addressState.copy(
                inputIsEnabled = false,
            ),
        )
    }
}

private class EmptyReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = sendState
}

private class PrepareSendScreenStatesReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        val prepareAction = action as PrepareSendScreen
        val walletManager = action.walletManager
        val amountToExtract = prepareAction.tokenAmount ?: prepareAction.coinAmount!!
        val decimals = amountToExtract.decimals
        val canIncludeFee = canIncludeFee(
            typeOfAmount = amountToExtract.type,
            feePaidCurrency = action.feePaidCurrency,
        )

        return sendState.copy(
            walletManager = walletManager,
            coinConverter = action.coinRate?.let { CurrencyConverter(it, decimals) },
            tokenConverter = action.tokenRate?.let { CurrencyConverter(it, decimals) },
            customFeeConverter = action.feeCurrencyRate?.let { CurrencyConverter(it, action.feeCurrencyDecimals) },
            amountState = sendState.amountState.copy(
                amountToExtract = amountToExtract,
                typeOfAmount = amountToExtract.type,
                balanceCrypto = amountToExtract.value ?: BigDecimal.ZERO,
            ),
            feeState = sendState.feeState.copy(includeFeeSwitcherIsEnabled = canIncludeFee),
            canIncludeFee = canIncludeFee,
            currency = action.currency,
        )
    }

    private fun canIncludeFee(typeOfAmount: AmountType, feePaidCurrency: FeePaidCurrency): Boolean {
        return when (feePaidCurrency) {
            FeePaidCurrency.Coin -> typeOfAmount == AmountType.Coin
            FeePaidCurrency.SameCurrency -> true
            is FeePaidCurrency.Token -> {
                val sendToken = (typeOfAmount as? AmountType.Token)?.token ?: return false

                sendToken.contractAddress.equals(feePaidCurrency.token.contractAddress, ignoreCase = true) &&
                    sendToken.name.equals(feePaidCurrency.token.name, ignoreCase = true) &&
                    sendToken.symbol.equals(feePaidCurrency.token.symbol, ignoreCase = true)
            }
        }
    }
}

internal fun updateLastState(sendState: SendState, lastChangedState: IdStateHolder): SendState {
    sendState.lastChangedStates.add(lastChangedState.stateId)
    return sendState
}