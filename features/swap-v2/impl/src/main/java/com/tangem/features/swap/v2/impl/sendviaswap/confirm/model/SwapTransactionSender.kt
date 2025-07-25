package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapDataTransactionModel
import com.tangem.domain.swap.usecase.GetSwapDataUseCase
import com.tangem.domain.swap.usecase.SwapTransactionSentUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import com.tangem.features.swap.v2.impl.common.ConfirmData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SwapTransactionSender @AssistedInject constructor(
    private val getSwapDataUseCase: GetSwapDataUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val swapTransactionSentUseCase: SwapTransactionSentUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    @Assisted private val userWallet: UserWallet,
) {

    suspend fun sendTransaction(
        confirmData: ConfirmData,
        isAmountSubtractAvailable: Boolean,
        onSendSuccess: (String, Long, SwapDataModel) -> Unit,
        onExpressError: (ExpressError) -> Unit,
        onSendError: (SendTransactionError?) -> Unit,
    ) {
        val provider = confirmData.quote?.provider ?: return

        when (val providerType = provider.type) {
            ExpressProviderType.CEX -> onCexTransaction(
                confirmData = confirmData,
                isAmountSubtractAvailable = isAmountSubtractAvailable,
                onExpressError = onExpressError,
                onSendSuccess = onSendSuccess,
                onSendError = onSendError,
            )
            ExpressProviderType.DEX,
            ExpressProviderType.DEX_BRIDGE,
            ExpressProviderType.ONRAMP,
            -> {
                Timber.w("Provider $providerType is not supported in Send With Swap")
                onExpressError(ExpressError.UnknownError)
            }
        }
    }

    suspend fun onCexTransaction(
        confirmData: ConfirmData,
        isAmountSubtractAvailable: Boolean,
        onSendSuccess: (String, Long, SwapDataModel) -> Unit,
        onExpressError: (ExpressError) -> Unit,
        onSendError: (SendTransactionError?) -> Unit,
    ) {
        val fromStatus = confirmData.fromCryptoCurrencyStatus ?: return
        val toStatus = confirmData.toCryptoCurrencyStatus ?: return
        val provider = confirmData.quote?.provider ?: return
        val rateType = confirmData.rateType ?: return
        val amountValue = confirmData.enteredAmount ?: return
        val feeValue = confirmData.fee?.amount?.value ?: return

        val fromAmount = FeeCalculationUtils.checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = fromStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = confirmData.reduceAmountBy,
        )

        val swapData = getSwapDataUseCase(
            userWallet = userWallet,
            fromCryptoCurrencyStatus = fromStatus,
            fromAmount = fromAmount.toStringWithRightOffset(fromStatus.currency.decimals),
            toCryptoCurrencyStatus = toStatus,
            toAddress = confirmData.enteredDestination,
            expressProvider = provider,
            rateType = rateType,
        ).getOrElse { onExpressError(it); return }

        createAndSendCexTransaction(
            fromAmount = fromAmount,
            fromStatus = fromStatus,
            toStatus = toStatus,
            fee = confirmData.fee,
            provider = provider,
            swapData = swapData,
            onSendSuccess = onSendSuccess,
            onExpressError = onExpressError,
            onSendError = onSendError,
        )
    }

    private suspend fun createAndSendCexTransaction(
        fromAmount: BigDecimal,
        fromStatus: CryptoCurrencyStatus,
        toStatus: CryptoCurrencyStatus,
        fee: Fee,
        provider: ExpressProvider,
        swapData: SwapDataModel,
        onSendSuccess: (String, Long, SwapDataModel) -> Unit,
        onExpressError: (ExpressError) -> Unit,
        onSendError: (SendTransactionError?) -> Unit,
    ) {
        val swapTransaction = if (swapData.transaction is SwapDataTransactionModel.CEX) {
            swapData.transaction
        } else {
            onExpressError(ExpressError.UnknownError)
            return
        }

        val txData = createTransferTransactionUseCase(
            amount = fromAmount.convertToSdkAmount(fromStatus.currency),
            fee = fee,
            memo = swapTransaction.txExtraId,
            destination = swapTransaction.txTo,
            userWalletId = userWallet.walletId,
            network = fromStatus.currency.network,
        ).getOrElse {
            Timber.e(it, "Failed to create swap CEX tx data")
            onSendError(SendTransactionError.UnknownError(Exception(it)))
            return
        }

        if (txData.extras == null && swapTransaction.txExtraId != null) {
            onExpressError(ExpressError.UnknownError)
            return
        }

        sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = fromStatus.currency.network,
        ).fold(
            ifLeft = { onSendError(it) },
            ifRight = { txHash ->
                val timestamp = System.currentTimeMillis()
                swapTransactionSentUseCase.invoke(
                    userWallet = userWallet,
                    fromCryptoCurrencyStatus = fromStatus,
                    toCryptoCurrencyStatus = toStatus,
                    swapDataTransactionModel = swapTransaction,
                    provider = provider,
                    txHash = txHash,
                    timestamp = timestamp,
                )
                onSendSuccess(txHash, timestamp, swapData)
            },
        )
    }

    private fun BigDecimal.toStringWithRightOffset(decimals: Int): String {
        return movePointRight(decimals).toPlainString()
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): SwapTransactionSender
    }
}