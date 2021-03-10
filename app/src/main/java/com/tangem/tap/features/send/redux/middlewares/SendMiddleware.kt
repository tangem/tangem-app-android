package com.tangem.tap.features.send.redux.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.stellar.StellarTransactionExtras
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.commands.common.card.Card
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.TapWorkarounds
import com.tangem.tap.domain.extensions.minimalAmount
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import com.tangem.tap.features.send.redux.states.SendButtonState
import com.tangem.tap.features.send.redux.states.TransactionExtrasState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import java.util.*

/**
* [REDACTED_AUTHOR]
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
                is PrepareSendScreen -> setIfSendingToPayIdEnabled(appState(), dispatch)
            }
            nextDispatch(action)
        }
    }
}

private fun verifyAndSendTransaction(
        action: SendActionUi.SendAmountToRecipient, appState: AppState?, dispatch: (Action) -> Unit,
) {
    val sendState = appState?.sendState ?: return
    val walletManager = appState.globalState.scanNoteResponse?.walletManager ?: return
    val card = appState.globalState.scanNoteResponse.card
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
                sendTransaction(action, walletManager, amountToSend, feeAmount, destinationAddress,
                        sendState.transactionExtrasState, card, dispatch)
            }, reduceAmount))
        }
        transactionErrors.isNotEmpty() -> {
            dispatch(SendAction.SendError(createValidateTransactionError(transactionErrors, walletManager)))
        }
        else -> {
            sendTransaction(action, walletManager, amountToSend, feeAmount, destinationAddress,
                    sendState.transactionExtrasState, card, dispatch)
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
        dispatch: (Action) -> Unit,
) {
    dispatch(SendAction.ChangeSendButtonState(SendButtonState.PROGRESS))
    var txData = walletManager.createTransaction(amountToSend, feeAmount, destinationAddress)

    transactionExtras.xlmMemo?.memo?.let { txData = txData.copy(extras = StellarTransactionExtras(it)) }
    transactionExtras.xrpDestinationTag?.tag?.let { txData = txData.copy(extras = XrpTransactionBuilder.XrpTransactionExtras(it)) }

    scope.launch {
        walletManager.update()
        val isLinkedTerminal = tangemSdk.config.linkedTerminal
        if (TapWorkarounds.isStart2Coin) {
            tangemSdk.config.linkedTerminal = false
        }
        val signer = Signer(tangemSdk, action.messageForSigner)
        val result = (walletManager as TransactionSender).send(txData, signer)
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> {
                    tangemSdk.config.linkedTerminal = isLinkedTerminal
                    FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.TRANSACTION_IS_SENT, card)
                    dispatch(SendAction.SendSuccess)
                    dispatch(GlobalAction.UpdateWalletSignedHashes(result.data.walletSignedHashes))
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
                            result.error?.let {
                                FirebaseCrashlytics.getInstance().recordException(it)
                            }
                        }
                        is Throwable -> {
                            val throwable = result.error as Throwable
                            val message = throwable.message
                            when {
                                message == null -> {
                                    dispatch(SendAction.SendError(TapError.UnknownError))
                                    updateFeedbackManager(walletManager, amountToSend, feeAmount, destinationAddress, card)
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
                                    Timber.e(throwable)
                                    FirebaseCrashlytics.getInstance().recordException(throwable)
                                    dispatch(SendAction.SendError(TapError.CustomError(message)))
                                    updateFeedbackManager(walletManager, amountToSend, feeAmount, destinationAddress, card)
                                    dispatch(SendAction.Dialog.SendTransactionFails(message))
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

private fun updateFeedbackManager(
        walletManager: WalletManager,
        amountToSend: Amount,
        feeAmount: Amount,
        destinationAddress: String,
        card: Card,
) {
    val infoHolder = store.state.globalState.feedbackManager?.infoHolder ?: return
    val amountState = store.state.sendState.amountState

    infoHolder.cardId = card.cardId
    infoHolder.blockchain = walletManager.wallet.blockchain
    infoHolder.sourceAddress = walletManager.wallet.address
    infoHolder.destinationAddress = destinationAddress
    infoHolder.amount = amountToSend.value?.stripZeroPlainString() ?: "0"
    infoHolder.fee = feeAmount.value?.stripZeroPlainString() ?: "0"
    infoHolder.cardFirmwareVersion = card.firmwareVersion.version
    if (amountState.typeOfAmount is AmountType.Token) {
        infoHolder.token = amountState.amountToExtract?.currencySymbol ?: ""
    }
//    infoHolder.transactionHex = ""
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


