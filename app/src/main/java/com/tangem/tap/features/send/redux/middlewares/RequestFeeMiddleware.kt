package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.ReceiptAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.DispatchFunction
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
class RequestFeeMiddleware {

    @Suppress("CyclomaticComplexMethod")
    fun handle(appState: AppState?, dispatch: DispatchFunction) {
        val sendState = appState?.sendState ?: return
        val walletManager = sendState.walletManager ?: return
        val scanResponse = appState.globalState.scanResponse ?: return

        if (!SendState.isReadyToRequestFee()) {
            dispatch(FeeAction.FeeCalculation.ClearResult)
            dispatch(ReceiptAction.RefreshReceipt)
            dispatch(SendAction.ChangeSendButtonState(sendState.getButtonState()))
            return
        }
        val typedAmount = sendState.amountState.amountToExtract ?: return

        val destinationAddress = sendState.addressState.destinationWalletAddress!!
        val destinationAmount = Amount(typedAmount, sendState.amountState.amountToSendCrypto)
        val txSender = if (scanResponse.isDemoCard()) {
            DemoTransactionSender(walletManager)
        } else {
            walletManager
        }
        scope.launch {
            val feeResult = txSender.getFee(destinationAmount, destinationAddress)
            withContext(Dispatchers.Main) {
                when (feeResult) {
                    is Result.Success -> {
                        val result = feeResult.data
//                    val result = FeeMock.getFee(walletManager.wallet.blockchain)
                        dispatch(FeeAction.FeeCalculation.SetFeeResult(result))
                        when (result) {
                            is TransactionFee.Single -> {
                                val fee = result.normal.amount.value ?: BigDecimal.ZERO
                                if (fee.isZero()) {
                                    dispatch(FeeAction.ChangeLayoutVisibility(main = false))
                                } else {
                                    dispatch(FeeAction.ChangeLayoutVisibility(main = true, chipGroup = false))
                                }
                            }
                            is TransactionFee.Choosable -> {
                                dispatch(FeeAction.ChangeLayoutVisibility(main = true, chipGroup = true))
                            }
                        }
                    }
                    is Result.Failure -> {
                        dispatch(FeeAction.FeeCalculation.ClearResult)
                        dispatch(FeeAction.ChangeLayoutVisibility(main = false))

                        val featureToggles = store.inject(DaggerGraphState::feedbackManagerFeatureToggles)
                        if (featureToggles.isLocalLogsEnabled) {
                            saveBlockchainError(feeResult, destinationAddress, destinationAmount, walletManager)
                        } else {
                            store.state.globalState.feedbackManager?.infoHolder?.updateOnSendError(
                                walletManager = walletManager,
                                amountToSend = destinationAmount,
                                feeAmount = null,
                                destinationAddress = destinationAddress,
                            )
                        }

                        val blockchainSdkError = feeResult.error as? BlockchainSdkError ?: return@withContext
                        dispatch(
                            SendAction.Dialog.RequestFeeError(
                                error = blockchainSdkError,
                                scanResponse = scanResponse,
                                onRetry = { dispatch(FeeAction.RequestFee) },
                            ),
                        )
                    }
                }
                dispatch(AmountActionUi.CheckAmountToSend)
            }
        }
    }

    private fun saveBlockchainError(
        feeResult: Result.Failure,
        destinationAddress: String,
        destinationAmount: Amount,
        walletManager: WalletManager,
    ) {
        store.inject(DaggerGraphState::saveBlockchainErrorUseCase).invoke(
            error = BlockchainErrorInfo(
                errorMessage = (feeResult.error as? BlockchainSdkError)?.customMessage
                    ?: "It isn't BlockchainSdkError",
                blockchainId = walletManager.wallet.blockchain.id,
                derivationPath = walletManager.wallet.publicKey.derivationPath?.rawPath ?: "",
                destinationAddress = destinationAddress,
                tokenSymbol = if (destinationAmount.type is AmountType.Token) {
                    destinationAmount.currencySymbol
                } else {
                    ""
                },
                amount = destinationAmount.value?.stripZeroPlainString() ?: "0",
                fee = null,
            ),
        )
    }
}
