package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.DataError
import com.tangem.feature.swap.domain.models.data.SwapState
import com.tangem.feature.swap.domain.models.data.TransactionModel
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import java.math.BigDecimal
import javax.inject.Inject

internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val cache: SwapDataCache,
) : SwapInteractor {

    override suspend fun getTokensToSwap(networkId: String): List<Currency> {
        val availableTokens = cache.getAvailableTokens(networkId)
        return availableTokens.ifEmpty {
            val tokens = repository.getExchangeableTokens(networkId)
            cache.cacheAvailableToSwapTokens(networkId, tokens)
            cache.cacheNetworkId(networkId)
            tokens
        }
    }

    override suspend fun getTokenBalance(tokenId: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun givePermissionToSwap(tokenAddress: String) {
        cache.getNetworkId()?.let { networkId ->
            val oneInchAddress = repository.addressForTrust(networkId)
            val transactionData = repository.dataToApprove(networkId, tokenAddress)
            //todo for test
            // transactionManager.sendTransaction(
            //     networkId = networkId,
            //     amountToSend = BigDecimal.ZERO,
            //     currencyToSend = ,
            //     feeAmount = BigDecimal.ZERO,
            //     destinationAddress = oneInchAddress,
            //     dataToSign = transactionData.data
            // )
        }
    }

    override suspend fun findBestQuote(fromTokenAddress: String, toTokenAddress: String, amount: String): SwapState {
        val networkId = cache.getNetworkId()
        val isAllowedToSpend = if (networkId.isNullOrEmpty()) {
            error("no networkId found, please call getTokensToSwap first")
        } else {
            val allowance =
                repository.checkTokensSpendAllowance(
                    networkId = networkId,
                    tokenAddress = fromTokenAddress,
                    walletAddress = userWalletManager.getWalletAddress(networkId),
                )
            allowance.error == DataError.NO_ERROR && allowance.dataModel != "0"
        }
        repository.findBestQuote(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount,
        ).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                cache.cacheSwapParams(quoteDataModel.copy(isAllowedToSpend = isAllowedToSpend), amount)
                return quoteDataModel
            } else {
                return SwapState.SwapError(quotes.error)
            }
        }
    }

    override suspend fun onSwap(): SwapState {
        val quoteModel = cache.getLastQuote()
        val amountToSwap = cache.getAmountToSwap()
        val networkId = cache.getNetworkId()
        if (quoteModel != null && amountToSwap != null && networkId != null) {
            repository.prepareSwapTransaction(
                networkId = networkId,
                fromTokenAddress = quoteModel.fromTokenAmount,
                toTokenAddress = quoteModel.toTokenAmount,
                amount = amountToSwap,
                slippage = DEFAULT_SLIPPAGE,
                fromWalletAddress = getWalletAddress(networkId),
            ).let {
                val swapData = it.dataModel
                if (swapData != null) {
                    signTransactionData(swapData.transaction) //todo implement
                    return SwapState.SwapSuccess(
                        quoteModel.fromTokenAmount,
                        quoteModel.toTokenAmount,
                    )
                } else {
                    return SwapState.SwapError(it.error)
                }
            }
        } else {
            throw IllegalStateException("cache is empty, call 'findBestQuote' first")
        }
    }

    private fun getWalletAddress(networkId: String): String {
        return userWalletManager.getWalletAddress(networkId)
    }

    //todo implement after merge referral
    private fun signTransactionData(transaction: TransactionModel) {
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
    }
}
