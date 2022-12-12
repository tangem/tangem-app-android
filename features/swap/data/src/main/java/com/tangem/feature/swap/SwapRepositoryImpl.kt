package com.tangem.feature.swap

import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.feature.swap.converters.ApproveConverter
import com.tangem.feature.swap.converters.QuotesConverter
import com.tangem.feature.swap.converters.SwapConverter
import com.tangem.feature.swap.converters.TokensConverter
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.feature.swap.domain.models.ApproveModel
import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.QuoteModel
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SwapRepositoryImpl @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val oneInchApi: OneInchApi,
    private val tokensConverter: TokensConverter,
    private val quotesConverter: QuotesConverter,
    private val swapConverter: SwapConverter,
    private val approveConverter: ApproveConverter,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
) : SwapRepository {

    override suspend fun getExchangeableTokens(networkId: String): List<Currency> {
        return withContext(coroutineDispatcher.io) {
            tokensConverter.convertList(tangemTechApi.coins(exchangeable = true, networkIds = networkId).coins)
        }
    }

    override suspend fun findBestQuote(fromTokenAddress: String, toTokenAddress: String, amount: String): QuoteModel {
        return withContext(coroutineDispatcher.io) {
            quotesConverter.convert(
                oneInchApi.quote(
                    fromTokenAddress = fromTokenAddress,
                    toTokenAddress = toTokenAddress,
                    amount = amount,
                ),
            )
        }
    }

    override suspend fun addressForTrust(): String {
        return withContext(coroutineDispatcher.io) {
            oneInchApi.approveSpender().address
        }
    }

    override suspend fun dataToApprove(tokenAddress: String, amount: String): ApproveModel {
        return withContext(coroutineDispatcher.io) {
            approveConverter.convert(oneInchApi.approveTransaction(tokenAddress, amount))
        }
    }

    override suspend fun checkTokensSpendAllowance(tokenAddress: String, walletAddress: String): String {
        return withContext(coroutineDispatcher.io) {
            oneInchApi.approveAllowance(tokenAddress, walletAddress).allowance
        }
    }

    override suspend fun prepareSwapTransaction(
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
        fromAddress: String,
        slippage: Int,
    ) {
        return withContext(coroutineDispatcher.io) {
            swapConverter.convert(
                oneInchApi.swap(
                    fromTokenAddress = fromTokenAddress,
                    toTokenAddress = toTokenAddress,
                    amount = amount,
                    fromAddress = fromAddress,
                    slippage = slippage,
                ),
            )
        }
    }
}
