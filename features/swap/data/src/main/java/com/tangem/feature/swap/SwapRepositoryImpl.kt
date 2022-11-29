package com.tangem.feature.swap

import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.feature.swap.converters.QuotesConverter
import com.tangem.feature.swap.converters.TokensConverter
import com.tangem.feature.swap.domain.SwapRepository
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
}