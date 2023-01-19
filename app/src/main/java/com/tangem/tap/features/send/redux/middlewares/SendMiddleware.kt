package com.tangem.tap.features.send.redux.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.binance.BinanceTransactionExtras
import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.blockchains.stellar.StellarTransactionExtras
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionError
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.extensions.minimalAmount
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoTransactionSender
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.send.redux.AddressPayIdActionUi
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.SendActionUi
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.send.redux.states.ExternalTransactionData
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.TransactionExtrasState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import java.util.*

/**
 * Created by Anton Zhilenkov on 02/09/2020.
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
                            store.dispatchOnMain(
                                AddressPayIdVerifyAction.AddressVerification.SetWalletAddress(
                                    address = transactionData.destinationAddress,
                                    isUserInput = false,
                                ),
                            )
                            store.dispatchOnMain(AmountActionUi.SetMainCurrency(MainCurrencyType.CRYPTO))
                            store.dispatchOnMain(AmountActionUi.HandleUserInput(transactionData.amount))
                            store.dispatchOnMain(
                                AmountAction.SetAmount(
                                    transactionData.amount.toBigDecimal(),
                                    false,
                                ),
                            )
                        }
                    }
                }
                nextDispatch(action)
            }
        }
    }
}

private fun verifyAndSendTransaction(
    action: SendActionUi.SendAmountToRecipient,
    appState: AppState?,
    dispatch: (Action) -> Unit,
) {
    val sendState = appState?.sendState ?: return
    val walletManager = sendState.walletManager ?: return
    val card = appState.globalState.scanResponse?.card ?: return
    val destinationAddress = sendState.addressPayIdState.destinationWalletAddress ?: return
    val typedAmount = sendState.amountState.amountToExtract ?: return
    val feeAmount = sendState.feeState.currentFee ?: return

    val amountToSend = Amount(typedAmount, sendState.getTotalAmountToSend())

    val transactionErrors = walletManager.validateTransaction(amountToSend, feeAmount)
    when {
        transactionErrors.contains(TransactionError.TezosSendAll) -> {
            val reduceAmount = walletManager.wallet.blockchain.minimalAmount()
            dispatch(
                SendAction.Dialog.TezosWarningDialog(
                    reduceCallback = {
                        dispatch(AmountAction.SetAmount(typedAmount.value!!.minus(reduceAmount), false))
                        dispatch(AmountActionUi.CheckAmountToSend)
                    },
                    sendAllCallback = {
                        sendTransaction(
                            action, walletManager, amountToSend, feeAmount, destinationAddress,
                            sendState.transactionExtrasState, card, sendState.externalTransactionData,
                            dispatch,
                        )
                    },
                    reduceAmount,
                ),
            )
        }
        else -> {
            sendTransaction(
                action, walletManager, amountToSend, feeAmount, destinationAddress,
                sendState.transactionExtrasState, card, sendState.externalTransactionData, dispatch,
            )
        }
    }
}

@Suppress("LongParameterList", "LongMethod", "ComplexMethod")
private fun sendTransaction(
    action: SendActionUi.SendAmountToRecipient,
    walletManager: WalletManager,
    amountToSend: Amount,
    feeAmount: Amount,
    destinationAddress: String,
    transactionExtras: TransactionExtrasState,
    card: CardDTO,
    externalTransactionData: ExternalTransactionData?,
    dispatch: (Action) -> Unit,
) {
    dispatch(SendAction.ChangeSendButtonState(ButtonState.PROGRESS))
    var txData = walletManager.createTransaction(amountToSend, feeAmount, destinationAddress)

    transactionExtras.xlmMemo?.memo?.let { txData = txData.copy(extras = StellarTransactionExtras(it)) }
    transactionExtras.binanceMemo?.memo?.let { txData = txData.copy(extras = BinanceTransactionExtras(it.toString())) }
    transactionExtras.xrpDestinationTag?.tag?.let {
        txData = txData.copy(extras = XrpTransactionBuilder.XrpTransactionExtras(it))
    }

    scope.launch {
        val updateWalletResult = walletManager.safeUpdate()
        if (updateWalletResult is Result.Failure) {
            when (val error = updateWalletResult.error) {
                is TapError -> store.dispatchErrorNotification(error)
                else -> {
                    val tapError = if (error.message == null) {
                        TapError.UnknownError
                    } else {
                        TapError.CustomError(error.message!!)
                    }
                    store.dispatchErrorNotification(tapError)
                }
            }
            withMainContext { dispatch(SendAction.ChangeSendButtonState(ButtonState.ENABLED)) }
            return@launch
        }

        val linkedTerminalState = tangemSdk.config.linkedTerminal
        if (card.isStart2Coin) {
            tangemSdk.config.linkedTerminal = false
        }

        val signer = TangemSigner(
            card = card,
            tangemSdk = tangemSdk,
            initialMessage = action.messageForSigner,
        ) { signResponse ->
            store.dispatch(
                GlobalAction.UpdateWalletSignedHashes(
                    walletSignedHashes = signResponse.totalSignedHashes,
                    walletPublicKey = walletManager.wallet.publicKey.seedKey,
                    remainingSignatures = signResponse.remainingSignatures,
                ),
            )
        }
        val sendResult = try {
            if (card.isDemoCard()) {
                DemoTransactionSender(walletManager).send(txData, signer)
            } else {
                (walletManager as TransactionSender).send(txData, signer)
            }
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            delay(DELAY_SDK_DIALOG_CLOSE)
            withMainContext {
                tangemSdk.config.linkedTerminal = linkedTerminalState
                dispatch(SendAction.ChangeSendButtonState(ButtonState.ENABLED))
                store.dispatchErrorNotification(TapError.CustomError(ex.localizedMessage ?: "Unknown error"))
            }
            return@launch
        }

        withMainContext {
            dispatch(SendAction.ChangeSendButtonState(ButtonState.ENABLED))
            tangemSdk.config.linkedTerminal = linkedTerminalState

            val currencyType = AnalyticsParam.CurrencyType.Amount(amountToSend)
            when (sendResult) {
                is SimpleResult.Success -> {
                    Analytics.send(Token.Send.TransactionSent(currencyType))
                    dispatch(SendAction.SendSuccess)

                    if (externalTransactionData != null) {
                        dispatch(WalletAction.TradeCryptoAction.FinishSelling(externalTransactionData.transactionId))
                    } else {
                        dispatch(NavigationAction.PopBackTo())
                    }
                    scope.launch(Dispatchers.IO) {
                        updateWallet(walletManager)
                        delay(timeMillis = 11000) // more than 10000 to avoid throttling
                        updateWallet(walletManager)
                    }
                }
                is SimpleResult.Failure -> {
                    store.state.globalState.feedbackManager?.infoHolder?.updateOnSendError(
                        walletManager = walletManager,
                        amountToSend = amountToSend,
                        feeAmount = feeAmount,
                        destinationAddress = destinationAddress,
                    )
                    val error = sendResult.error as? BlockchainSdkError ?: return@withMainContext

                    Analytics.send(Token.Send.TransactionSent(currencyType, error))

                    when (error) {
                        is BlockchainSdkError.WrappedTangemError -> {
                            val tangemSdkError = error.tangemError as? TangemSdkError ?: return@withMainContext
                            if (tangemSdkError is TangemSdkError.UserCancelled) return@withMainContext

                            dispatch(SendAction.Dialog.SendTransactionFails.CardSdkError(tangemSdkError))
                        }
                        is BlockchainSdkError.CreateAccountUnderfunded -> {
                            // from XLM, XRP, Polkadot
                            dispatch(SendAction.Dialog.SendTransactionFails.BlockchainSdkError(error))
                        }
                        else -> {
                            when {
                                error.customMessage.contains(DemoTransactionSender.ID) -> {
                                    store.dispatchDialogShow(
                                        AppDialog.SimpleOkDialogRes(
                                            headerId = R.string.common_done,
                                            messageId = R.string.alert_demo_feature_disabled,
                                            onOk = { dispatch(NavigationAction.PopBackTo()) },
                                        ),
                                    )
                                }
                                else -> {
                                    dispatch(SendAction.Dialog.SendTransactionFails.BlockchainSdkError(error))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createValidateTransactionError(
    errorList: EnumSet<TransactionError>,
    walletManager: WalletManager,
): TapError.ValidateTransactionErrors {
    val tapErrors = errorList.map {
        when (it) {
            TransactionError.AmountExceedsBalance -> TapError.AmountExceedsBalance
            TransactionError.AmountLowerExistentialDeposit -> {
                if (walletManager is ExistentialDepositProvider) {
                    val args = listOf(walletManager.getExistentialDeposit().stripZeroPlainString())
                    TapError.AmountLowerExistentialDeposit(args)
                } else {
                    TapError.UnknownError
                }
            }
            TransactionError.FeeExceedsBalance -> TapError.FeeExceedsBalance
            TransactionError.TotalExceedsBalance -> TapError.TotalExceedsBalance
            TransactionError.InvalidAmountValue -> TapError.InvalidAmountValue
            TransactionError.InvalidFeeValue -> TapError.InvalidFeeValue
            TransactionError.DustAmount -> {
                val args = listOf(walletManager.dustValue?.stripZeroPlainString() ?: "0")
                TapError.DustAmount(args)
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

private suspend fun updateWallet(walletManager: WalletManager) {
    val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
    if (selectedUserWallet != null) {
        val wallet = walletManager.wallet
        walletCurrenciesManager.update(
            userWallet = selectedUserWallet,
            currency = Currency.Blockchain(
                blockchain = wallet.blockchain,
                derivationPath = wallet.publicKey.derivationPath?.rawPath,
            ),
        )
    } else {
        store.dispatchOnMain(WalletAction.LoadWallet(BlockchainNetwork.fromWalletManager(walletManager)))
    }
}
