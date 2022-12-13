package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.models.SwapResultModel
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.QuoteModel
import com.tangem.feature.swap.domain.models.data.TransactionModel
import com.tangem.lib.crypto.UserWalletManager
import javax.inject.Inject

internal class SwapInteractorImpl @Inject constructor(
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

    override suspend fun findBestQuote(fromTokenAddress: String, toTokenAddress: String, amount: String): QuoteModel {
        val networkId = cache.getNetworkId()
        val isAllowedToSpend = if (networkId.isNullOrEmpty()) {
            false
        } else {
            repository.checkTokensSpendAllowance(fromTokenAddress, userWalletManager.getWalletAddress(networkId)) != "0"
        }
        repository.findBestQuote(fromTokenAddress, toTokenAddress, amount).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                cache.cacheSwapParams(quoteDataModel.copy(isAllowedToSpend = isAllowedToSpend), amount)
                return quoteDataModel
            } else {
                error("") //todo handle error in domain layer(task AND-2515)
            }
        }
    }

    override suspend fun onSwap(): SwapResultModel {
        val quoteModel = cache.getLastQuote()
        val amountToSwap = cache.getAmountToSwap()
        val networkId = cache.getNetworkId()
        if (quoteModel != null && amountToSwap != null && networkId != null) {
            val swapData = repository.prepareSwapTransaction(
                fromTokenAddress = quoteModel.fromTokenAmount,
                toTokenAddress = quoteModel.toTokenAmount,
                amount = amountToSwap,
                slippage = DEFAULT_SLIPPAGE,
                fromWalletAddress = getWalletAddress(networkId),
            ).dataModel
            if (swapData != null) {
                signTransactionData(swapData.transaction) //todo implement
                return SwapResultModel.SwapSuccess(
                    quoteModel.fromTokenAmount,
                    quoteModel.toTokenAmount,
                )
            } else {
                error("") //todo handle error in domain layer(task AND-2515)
            }
        }
        return SwapResultModel.SwapError(
            0,
            "",
        )
    }

    private fun getWalletAddress(networkId: String): String {
        return userWalletManager.getWalletAddress(networkId)
    }
// [REDACTED_TODO_COMMENT]
    private fun signTransactionData(transaction: TransactionModel) {
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
    }
}
