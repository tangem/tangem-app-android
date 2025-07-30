package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType.Companion.shouldStoreSwapTransaction
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.SwapTransactionRepository
import com.tangem.domain.swap.models.SwapDataTransactionModel
import com.tangem.domain.swap.models.SwapStatus
import com.tangem.domain.swap.models.SwapStatusModel
import com.tangem.domain.swap.models.SwapTransactionModel

@Suppress("LongParameterList")
class SwapTransactionSentUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val swapErrorResolver: SwapErrorResolver,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        toCryptoCurrencyStatus: CryptoCurrencyStatus,
        swapDataTransactionModel: SwapDataTransactionModel,
        provider: ExpressProvider,
        txHash: String,
        timestamp: Long,
    ) = Either.catch {
        swapRepositoryV2.swapTransactionSent(
            userWallet = userWallet,
            fromCryptoCurrencyStatus = fromCryptoCurrencyStatus,
            toAddress = swapDataTransactionModel.txTo,
            txId = swapDataTransactionModel.txId,
            txHash = txHash,
            txExtraId = swapDataTransactionModel.txExtraId,
        )
        if (provider.type.shouldStoreSwapTransaction()) {
            swapTransactionRepository.storeTransaction(
                userWalletId = userWallet.walletId,
                fromCryptoCurrency = fromCryptoCurrencyStatus.currency,
                toCryptoCurrency = toCryptoCurrencyStatus.currency,
                transaction = SwapTransactionModel(
                    txId = swapDataTransactionModel.txId,
                    provider = provider,
                    timestamp = timestamp,
                    fromCryptoAmount = swapDataTransactionModel.fromAmount,
                    toCryptoAmount = swapDataTransactionModel.toAmount,
                    status = SwapStatusModel(
                        providerId = provider.providerId,
                        status = SwapStatus.New,
                        txId = swapDataTransactionModel.txId,
                        txExternalUrl = (swapDataTransactionModel as? SwapDataTransactionModel.CEX)?.externalTxUrl,
                        txExternalId = (swapDataTransactionModel as? SwapDataTransactionModel.CEX)?.externalTxId,
                        averageDuration = null,
                    ),
                ),
            )
        }
        swapTransactionRepository.storeLastSwappedCryptoCurrencyId(
            userWalletId = userWallet.walletId,
            cryptoCurrencyId = toCryptoCurrencyStatus.currency.id,
        )
    }.mapLeft(swapErrorResolver::resolve)
}