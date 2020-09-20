package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.extensions.Signer
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.ChangeAddressOrPayId
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.*
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.SendActionUi
import com.tangem.tap.features.send.redux.states.SendButtonState
import com.tangem.tap.scope
import com.tangem.tap.tangemSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware

/**
[REDACTED_AUTHOR]
 */
val sendMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            when (action) {
                is ChangeAddressOrPayId -> AddressPayIdMiddleware().handle(action.data, appState(), dispatch)
                is VerifyClipboard -> {
                    AddressPayIdMiddleware().handle(action.data, appState()) {
                        when (it) {
                            is AddressVerification.SetWalletAddress, is PayIdVerification.SetPayIdWalletAddress -> {
                                dispatch(ChangePasteBtnEnableState(true))
                            }
                            is AddressVerification.SetError, is PayIdVerification.SetError -> {
                                dispatch(ChangePasteBtnEnableState(false))
                            }
                        }
                    }
                }
                is AmountActionUi -> AmountMiddleware().handle(action, appState(), dispatch)
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
    val amountToSend = Amount(sendState.getTotalAmountToSend(), blockchain, recipientAddress)

    val txSender = walletManager as TransactionSender

    val verifyResult = walletManager.validateTransaction(amountToSend, feeAmount)
    if (verifyResult.isNotEmpty()) {
        dispatch(SendAction.SendError(TapError.InsufficientBalance))
        return
    }

    dispatch(SendAction.ChangeSendButtonState(SendButtonState.PROGRESS))
    val txData = walletManager.createTransaction(amountToSend, feeAmount, recipientAddress)
    scope.launch {
        val result = txSender.send(txData, Signer(tangemSdk))
        withContext(Dispatchers.Main) {
            when (result) {
                is SimpleResult.Success -> {
                    dispatch(SendAction.SendSuccess)
                    dispatch(NavigationAction.PopBackTo())
                }
                is SimpleResult.Failure -> {
                    when (result.error) {
                        is Throwable -> {
                            val message = (result.error as Throwable).message
                            when {
                                message == null -> {
                                    dispatch(SendAction.SendError(TapError.UnknownError))
                                }
                                message.contains("50002") -> {
                                    // user was cancelled the operation by closing the Sdk bottom sheet
                                }
                                else -> {
                                    dispatch(SendAction.SendError(TapError.BlockchainInternalError))
                                }
                            }
                        }
                    }
                }
            }
            dispatch(SendAction.ChangeSendButtonState(SendButtonState.ENABLED))
        }
    }

}

