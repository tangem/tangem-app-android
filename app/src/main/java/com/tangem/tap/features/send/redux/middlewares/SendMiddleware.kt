package com.tangem.tap.features.send.redux.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.send.redux.AddressPayIdActionUi
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.SendActionUi
import com.tangem.tap.features.send.redux.states.SendButtonState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.tangemSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import java.util.*

/**
[REDACTED_AUTHOR]
 */
val sendMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            when (action) {
                is AddressPayIdActionUi -> AddressPayIdMiddleware().handle(action, appState(), dispatch)
                is AmountActionUi -> AmountMiddleware().handle(action, appState(), dispatch)
                is RequestFee -> RequestFeeMiddleware().handle(appState(), dispatch)
                is SendActionUi.SendAmountToRecipient ->
                    verifyAndSendTransaction(action, appState(), dispatch)
            }
            nextDispatch(action)
        }
    }
}

private fun verifyAndSendTransaction(
        action: SendActionUi.SendAmountToRecipient, appState: AppState?, dispatch: (Action) -> Unit
) {
    val sendState = appState?.sendState ?: return
    val walletManager = appState.globalState.scanNoteResponse?.walletManager ?: return
    val recipientAddress = sendState.addressPayIdState.recipientWalletAddress ?: return
    val typedAmount = sendState.amountState.amountToExtract ?: return
    val feeAmount = sendState.feeState.currentFee ?: return

    val amountToSend = Amount(typedAmount, sendState.getTotalAmountToSend())

    val verifyResult = walletManager.validateTransaction(amountToSend, feeAmount)
    if (verifyResult.isNotEmpty()) {
        dispatch(SendAction.SendError(createValidateTransactionError(verifyResult, walletManager)))
        return
    }

    dispatch(SendAction.ChangeSendButtonState(SendButtonState.PROGRESS))
    val txData = walletManager.createTransaction(amountToSend, feeAmount, recipientAddress)
    scope.launch {
        walletManager.update()
        val signer = Signer(tangemSdk, action.messageForSigner)
        val result = (walletManager as TransactionSender).send(txData, signer)
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> {
                    dispatch(SendAction.SendSuccess)
                    dispatch(GlobalAction.UpdateWalletSignedHashes(result.data.walletSignedHashes))
                    dispatch(WalletAction.UpdateWallet)
                    dispatch(NavigationAction.PopBackTo())
                }
                is Result.Failure -> {
                    when (result.error) {
                        is CreateAccountUnderfunded -> {
                            val error = result.error as CreateAccountUnderfunded
                            val reserve = error.minReserve.value?.stripZeroPlainString() ?: "0"
                            val symbol = error.minReserve.currencySymbol
                            dispatch(SendAction.SendError(TapError.CreateAccountUnderfunded(listOf(reserve, symbol))))
                        }
                        is SendException -> {
                            FirebaseCrashlytics.getInstance().recordException(result.error!!)
                        }
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
                                    Timber.e(result.error)
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

fun extractErrorsForAmountField(errors: EnumSet<TransactionError>): EnumSet<TransactionError> {
    val showIntoAmountField = EnumSet.noneOf(TransactionError::class.java)
    errors.forEach {
        when (it) {
            TransactionError.AmountExceedsBalance -> {
                showIntoAmountField.remove(TransactionError.TotalExceedsBalance)
                showIntoAmountField.add(it)
            }
            TransactionError.FeeExceedsBalance -> {
                showIntoAmountField.remove(TransactionError.TotalExceedsBalance)
                showIntoAmountField.add(it)
            }
            TransactionError.TotalExceedsBalance -> {
                val notAcceptable = listOf(TransactionError.FeeExceedsBalance, TransactionError.FeeExceedsBalance)
                if (!showIntoAmountField.containsAll(notAcceptable)) showIntoAmountField.add(it)
            }
            TransactionError.InvalidAmountValue -> showIntoAmountField.add(it)
            TransactionError.InvalidFeeValue -> showIntoAmountField.add(it)
        }
    }
    return showIntoAmountField
}

fun createValidateTransactionError(errorList: EnumSet<TransactionError>, walletManager: WalletManager): TapError.ValidateTransactionErrors {
    val tapErrors = errorList.map {
        when (it) {
            TransactionError.AmountExceedsBalance -> TapError.AmountExceedsBalance
            TransactionError.FeeExceedsBalance -> TapError.FeeExceedsBalance
            TransactionError.TotalExceedsBalance -> TapError.TotalExceedsBalance
            TransactionError.InvalidAmountValue -> TapError.InvalidAmountValue
            TransactionError.InvalidFeeValue -> TapError.InvalidFeeValue
            TransactionError.DustAmount -> {
                TapError.DustAmount(listOf(walletManager.dustValue?.stripZeroPlainString() ?: "0"))
            }
            TransactionError.DustChange -> TapError.DustChange
            else -> TapError.UnknownError
        }
    }
    return TapError.ValidateTransactionErrors(tapErrors) { it.joinToString("\r\n") }
}

