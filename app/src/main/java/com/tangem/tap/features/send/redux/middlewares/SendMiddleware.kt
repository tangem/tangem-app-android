package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.common.CompletionResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.ChangeAddressOrPayId
import com.tangem.tap.features.send.redux.AmountActionUi.CheckAmountToSend
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.SendActionUi
import com.tangem.tap.scope
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

/**
* [REDACTED_AUTHOR]
 */
val sendMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            when (action) {
                is ChangeAddressOrPayId -> AddressPayIdMiddleware().handle(action.data, appState(), dispatch)
                is CheckAmountToSend -> AmountMiddleware().handle(action.data, appState(), dispatch)
                is RequestFee -> RequestFeeMiddleware().handle(appState(), dispatch)
                is SendActionUi.SendAmountToRecipient -> verifyAndSendTransaction(appState(), dispatch)
            }
            nextDispatch(action)
        }
    }
}

private fun verifyAndSendTransaction(appState: AppState?, dispatch: (Action) -> Unit) {
    val sendState = appState?.sendState ?: return
    val walletManager = appState.globalState.scanNoteResponse?.walletManager ?: return

    val blockchain = walletManager.wallet.blockchain
    val recipientAddress = sendState.addressPayIdState.recipientWalletAddress!!

    val feeAmount = Amount(sendState.feeState.getCurrentFee(), blockchain)
    val amountToSend = Amount(sendState.amountState.amountToSendCrypto, blockchain, recipientAddress)

    scope.launch {
        when (val sendResult = tangemSdkManager.send(walletManager, recipientAddress, amountToSend, feeAmount)) {
            is CompletionResult.Success -> dispatch(SendAction.SendSuccess)
            is CompletionResult.Failure -> {
                when (sendResult.error.code) {
                    1021 -> dispatch(SendAction.SendError(SendAction.Error.INSUFFICIENT_BALANCE))
                    1001, 2000 -> dispatch(SendAction.SendError(SendAction.Error.BLOCKCHAIN_INTERNAL))
                }
            }
        }
    }

}


