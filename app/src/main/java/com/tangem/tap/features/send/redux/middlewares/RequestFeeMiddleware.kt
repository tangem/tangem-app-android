package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.demo.DemoTransactionSender
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.ReceiptAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.DispatchFunction
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class RequestFeeMiddleware {

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

        val destinationAddress = sendState.addressPayIdState.destinationWalletAddress!!
        val destinationAmount = Amount(typedAmount, sendState.amountState.amountToSendCrypto)
        val txSender = if (scanResponse.isDemoCard()) {
            DemoTransactionSender(walletManager)
        } else {
            walletManager as TransactionSender
        }
        scope.launch {
            val feeResult = txSender.getFee(destinationAmount, destinationAddress)
            withContext(Dispatchers.Main) {
                when (feeResult) {
                    is Result.Success -> {
                        val result = feeResult.data
//                    val result = FeeMock.getFee(walletManager.wallet.blockchain)
                        dispatch(FeeAction.FeeCalculation.SetFeeResult(result))
                        if (result.size == 1) {
                            val fee = result[0].value ?: BigDecimal.ZERO
                            if (fee.isZero()) {
                                dispatch(FeeAction.ChangeLayoutVisibility(main = false))
                            } else {
                                dispatch(FeeAction.ChangeLayoutVisibility(main = true, chipGroup = false))
                            }
                        } else {
                            dispatch(FeeAction.ChangeLayoutVisibility(main = true, chipGroup = true))
                        }
                    }
                    is Result.Failure -> {
                        dispatch(FeeAction.FeeCalculation.ClearResult)
                        dispatch(FeeAction.ChangeLayoutVisibility(main = false))

                        val blockchainSdkError = feeResult.error as? BlockchainSdkError ?: return@withContext
                        dispatch(
                            SendAction.Dialog.RequestFeeError(
                                error = blockchainSdkError,
                                onRetry = { dispatch(FeeAction.RequestFee) },
                            ),
                        )
                    }
                }
                dispatch(AmountActionUi.CheckAmountToSend)
            }
        }
    }
}

class FeeMock {
    companion object {
        suspend fun getFee(blockchain: Blockchain): List<Amount> {
            return feeStandard(blockchain)
//            return feeSingle(blockchain)
//            return feeStellar(blockchain)
//            return feeZero(blockchain)
        }

        suspend fun feeStellar(blockchain: Blockchain): List<Amount> = listOf(Amount(0.0001.toBigDecimal(), blockchain))

        suspend fun feeZero(blockchain: Blockchain): List<Amount> = listOf(Amount(BigDecimal.ZERO, blockchain))

        suspend fun feeStandard(blockchain: Blockchain): List<Amount> = listOf(
            Amount(0.001500.toBigDecimal(), blockchain),
            Amount(0.0030.toBigDecimal(), blockchain),
            Amount(0.0045001.toBigDecimal(), blockchain),
        )

        suspend fun feeStandardBig(blockchain: Blockchain): List<Amount> = listOf(
            Amount(1.76.toBigDecimal(), blockchain),
            Amount(2.30.toBigDecimal(), blockchain),
            Amount(3.45.toBigDecimal(), blockchain),
        )

        suspend fun feeSingle(blockchain: Blockchain): List<Amount> = listOf(Amount(0.0015.toBigDecimal(), blockchain))
    }
}