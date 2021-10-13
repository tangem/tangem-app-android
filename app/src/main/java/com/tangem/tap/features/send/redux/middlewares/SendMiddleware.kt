package com.tangem.tap.features.send.redux.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.stellar.StellarTransactionExtras
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.extensions.minimalAmount
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.send.redux.states.ExternalTransactionData
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.TransactionExtrasState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class SendMiddleware {
    val sendMiddleware: Middleware<AppState> = { dispatch, appState ->
        { nextDispatch ->
            { action ->
                when (action) {
                    is AddressPayIdActionUi -> AddressPayIdMiddleware().handle(action, appState(), dispatch)
                    is AmountActionUi -> AmountMiddleware().handle(action, appState(), dispatch)
                    is RequestFee -> RequestFeeMiddleware().handle(appState(), dispatch)
                    is SendActionUi.SendAmountToRecipient ->
                        verifyAndSendTransaction(action, appState(), dispatch)
                    is PrepareSendScreen -> setIfSendingToPayIdEnabled(appState(), dispatch)
                    is SendAction.Warnings.Update -> updateWarnings(dispatch)
                    is SendActionUi.CheckIfTransactionDataWasProvided -> {
                        val transactionData = appState()?.sendState?.externalTransactionData
                        if (transactionData != null) {
                            store.dispatchOnMain(AddressPayIdVerifyAction.AddressVerification.SetWalletAddress(
                                    transactionData.destinationAddress, false
                            ))
                            store.dispatchOnMain(AmountActionUi.SetMainCurrency(MainCurrencyType.CRYPTO))
                            store.dispatchOnMain(AmountActionUi.HandleUserInput(transactionData.amount))
                            store.dispatchOnMain(AmountAction.SetAmount(transactionData.amount.toBigDecimal(),
                                    false))
                        }
                    }
                }
                nextDispatch(action)
            }
        }
    }

}
private fun verifyAndSendTransaction(
        action: SendActionUi.SendAmountToRecipient, appState: AppState?, dispatch: (Action) -> Unit,
) {
    val sendState = appState?.sendState ?: return
    val walletManager = sendState.walletManager ?: return
    val card = appState.globalState.scanResponse?.card ?: return
    val destinationAddress = sendState.addressPayIdState.destinationWalletAddress ?: return
    val typedAmount = sendState.amountState.amountToExtract ?: return
    val feeAmount = sendState.feeState.currentFee ?: return

    val amountToSend = Amount(typedAmount, sendState.getTotalAmountToSend())

    val transactionErrors = walletManager.validateTransaction(amountToSend, feeAmount)
    val hadTezosError = transactionErrors.remove(TransactionError.TezosSendAll)
    when {
        hadTezosError -> {
            val reduceAmount = walletManager.wallet.blockchain.minimalAmount()
            dispatch(SendAction.Dialog.TezosWarningDialog(reduceCallback = {
                dispatch(AmountAction.SetAmount(typedAmount.value!!.minus(reduceAmount), false))
                dispatch(AmountActionUi.CheckAmountToSend)
            }, sendAllCallback = {
                sendTransaction(
                    action, walletManager, amountToSend, feeAmount, destinationAddress,
                    sendState.transactionExtrasState, card, sendState.externalTransactionData,
                    dispatch
                )
            }, reduceAmount))
        }
        transactionErrors.isNotEmpty() -> {
            dispatch(SendAction.SendError(createValidateTransactionError(transactionErrors, walletManager)))
        }
        else -> {
            sendTransaction(action, walletManager, amountToSend, feeAmount, destinationAddress,
                    sendState.transactionExtrasState, card, sendState.externalTransactionData, dispatch)
        }
    }
}

private fun sendTransaction(
        action: SendActionUi.SendAmountToRecipient,
        walletManager: WalletManager,
        amountToSend: Amount,
        feeAmount: Amount,
        destinationAddress: String,
        transactionExtras: TransactionExtrasState,
        card: Card,
        externalTransactionData: ExternalTransactionData?,
        dispatch: (Action) -> Unit,
) {
    dispatch(SendAction.ChangeSendButtonState(ButtonState.PROGRESS))
    var txData = walletManager.createTransaction(amountToSend, feeAmount, destinationAddress)

    transactionExtras.xlmMemo?.memo?.let { txData = txData.copy(extras = StellarTransactionExtras(it)) }
    transactionExtras.xrpDestinationTag?.tag?.let { txData = txData.copy(extras = XrpTransactionBuilder.XrpTransactionExtras(it)) }

    scope.launch {
        walletManager.update()
        val isLinkedTerminal = tangemSdk.config.linkedTerminal
        if (card.isStart2Coin) {
            tangemSdk.config.linkedTerminal = false
        }
        val signer = TangemSigner(
            tangemSdk = tangemSdk, initialMessage = action.messageForSigner
        ) { signResponse ->
            store.dispatch(
                GlobalAction.UpdateWalletSignedHashes(
                    walletSignedHashes = signResponse.totalSignedHashes,
                    walletPublicKey = walletManager.wallet.publicKey,
                    remainingSignatures = signResponse.remainingSignatures
                )
            )
        }
        val result = (walletManager as TransactionSender).send(txData, signer)
        withContext(Dispatchers.Main) {
            when (result) {
                is SimpleResult.Success -> {
                    tangemSdk.config.linkedTerminal = isLinkedTerminal
                    FirebaseAnalyticsHandler.triggerEvent(
                        event = AnalyticsEvent.TRANSACTION_IS_SENT,
                        card = card,
                        blockchain = walletManager.wallet.blockchain.currency
                    )
                    dispatch(SendAction.SendSuccess)

                    if (externalTransactionData != null) {
                        dispatch(WalletAction.TradeCryptoAction.FinishSelling(externalTransactionData.transactionId))
                    } else {
                        dispatch(NavigationAction.PopBackTo())
                    }
                    scope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            dispatch(WalletAction.UpdateWallet(walletManager.wallet.blockchain))
                        }
                        delay(10000)
                        withContext(Dispatchers.Main) {
                            dispatch(WalletAction.UpdateWallet(walletManager.wallet.blockchain))
                        }
                    }
                }
                is SimpleResult.Failure -> {
                    when (result.error) {
                        is CreateAccountUnderfunded -> {
                            val error = result.error as CreateAccountUnderfunded
                            val reserve = error.minReserve.value?.stripZeroPlainString() ?: "0"
                            val symbol = error.minReserve.currencySymbol
                            dispatch(SendAction.SendError(TapError.CreateAccountUnderfunded(listOf(reserve, symbol))))
                        }
                        is SendException -> {
                            result.error?.let {
                                FirebaseCrashlytics.getInstance().recordException(it)
                            }
                        }
                        is Throwable -> {
                            val throwable = result.error as Throwable
                            val message = throwable.message
                            val infoHolder = store.state.globalState.feedbackManager?.infoHolder
                            when {
                                message == null -> {
                                    dispatch(SendAction.SendError(TapError.UnknownError))
                                    infoHolder?.updateOnSendError(
                                        wallet = walletManager.wallet,
                                        host = walletManager.currentHost,
                                        amountToSend = amountToSend,
                                        feeAmount = feeAmount,
                                        destinationAddress = destinationAddress
                                    )
                                    dispatch(SendAction.Dialog.SendTransactionFails("unknown error"))
                                }
                                message.contains("50002") -> {
                                    // user was cancelled the operation by closing the Sdk bottom sheet
                                }
                                // make it easier latter by handling an appropriate enumError or, like on iOS,
                                // accept a string identifier of the error message
                                message.contains("Target account is not created. To create account send 1+ XLM.") -> {
                                    dispatch(SendAction.SendError(TapError.XmlError.AssetAccountNotCreated))
                                }
                                else -> {
                                    (result.error as? TangemSdkError)?.let { error ->
                                        FirebaseAnalyticsHandler.logCardSdkError(
                                            error,
                                            FirebaseAnalyticsHandler.ActionToLog.SendTransaction,
                                            mapOf(
                                                FirebaseAnalyticsHandler.AnalyticsParam.BLOCKCHAIN
                                                        to walletManager.wallet.blockchain.currency),
                                            card = card,
                                        )
                                    }
                                    Timber.e(throwable)
                                    FirebaseCrashlytics.getInstance().recordException(throwable)
                                    dispatch(SendAction.SendError(TapError.CustomError(message)))
                                    infoHolder?.updateOnSendError(
                                        wallet = walletManager.wallet,
                                        host = walletManager.currentHost,
                                        amountToSend = amountToSend,
                                        feeAmount = feeAmount,
                                        destinationAddress = destinationAddress
                                    )
                                    dispatch(SendAction.Dialog.SendTransactionFails(message))
                                }
                            }
                        }
                    }
                }
            }
            dispatch(SendAction.ChangeSendButtonState(ButtonState.ENABLED))
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

private fun setIfSendingToPayIdEnabled(appState: AppState?, dispatch: (Action) -> Unit) {
    val isSendingToPayIdEnabled =
            appState?.globalState?.configManager?.config?.isSendingToPayIdEnabled ?: false
    dispatch(AddressPayIdActionUi.ChangePayIdState(isSendingToPayIdEnabled))
}

private fun updateWarnings(dispatch: (Action) -> Unit) {
    val warningsManager = store.state.globalState.warningManager ?: return
    val blockchain = store.state.sendState.walletManager?.wallet?.blockchain ?: return

    val warnings = warningsManager.getWarnings(WarningMessage.Location.SendScreen, listOf(blockchain))
    dispatch(SendAction.Warnings.Set(warnings))
}

