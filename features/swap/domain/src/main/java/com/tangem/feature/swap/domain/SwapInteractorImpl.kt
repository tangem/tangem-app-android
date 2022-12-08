package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.SwapDataHolder
import com.tangem.feature.swap.domain.models.SwapResultModel
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.QuoteModel
import com.tangem.feature.swap.domain.models.data.TransactionModel

//todo add @Inject after merge referral and add HiltCore
class SwapInteractorImpl(
    private val repository: SwapRepository,
) : SwapInteractor {

    private var lastDataForSwap: SwapDataHolder? = null

    override suspend fun getTokensToSwap(networkId: String): List<Currency> {
        return repository.getExchangeableTokens(networkId)
    }

    override suspend fun getTokenBalance(): String {
        TODO("Not yet implemented")
    }

    override suspend fun findBestQuote(fromTokenAddress: String, toTokenAddress: String, amount: String): QuoteModel {
        repository.findBestQuote(fromTokenAddress, toTokenAddress, amount).let {
            lastDataForSwap = SwapDataHolder(it, amount)
            return it
        }
    }

    override suspend fun onSwap(): SwapResultModel {
        val dataForSwap = lastDataForSwap
        if (dataForSwap != null) {
            val swapData = repository.prepareSwapTransaction(
                fromTokenAddress = dataForSwap.quoteModel.fromTokenAmount,
                toTokenAddress = dataForSwap.quoteModel.toTokenAmount,
                amount = dataForSwap.amount,
                slippage = DEFAULT_SLIPPAGE,
                fromWalletAddress = getWalletAddress(),
            )
            signTransactionData(swapData.transaction) //todo implement
            return SwapResultModel.SwapSuccess(
                dataForSwap.quoteModel.fromTokenAmount,
                dataForSwap.quoteModel.toTokenAmount,
            )
        }
        return SwapResultModel.SwapError(
            0,
            "",
        )
    }

    //todo implement after merge referral
    private fun getWalletAddress(): String {
        return ""
    }

    //todo implement after merge referral
    private fun signTransactionData(transaction: TransactionModel) {
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 1
    }
}