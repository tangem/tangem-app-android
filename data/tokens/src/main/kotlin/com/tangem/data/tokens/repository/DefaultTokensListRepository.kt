package com.tangem.data.tokens.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.tokens.paging.CoinsPagingSource
import com.tangem.data.tokens.utils.FoundTokenConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.tokens.model.FoundToken
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensListRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Default repository implementation for managing operations related to a complete set of tokens
 *
 * @property tangemTechApi                Tangem Tech API
 * @property dispatchers                  coroutine dispatchers provider
 * @property quotesRepository             responsible for providing cryptocurrency quotes data
 *
 */
internal class DefaultTokensListRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val quotesRepository: QuotesRepository,
) : TokensListRepository {

    override fun getTokens(searchText: String?): Flow<PagingData<Token>> {
        return Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 70,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                CoinsPagingSource(
                    api = tangemTechApi,
                    dispatchers = dispatchers,
                    searchText = searchText,
                    quotesRepository = quotesRepository,
                )
            },
        ).flow
    }

    override suspend fun findToken(contractAddress: String, networkId: String): FoundToken? {
        return withContext(dispatchers.io) {
            val foundCoin = tangemTechApi.getCoins(
                contractAddress = contractAddress,
                networkIds = networkId,
            ).getOrThrow().coins.firstNotNullOfOrNull { coin ->
                val tokenNetwork = coin.networks.filter { network ->
                    network.contractAddress != null && network.decimalCount != null &&
                        network.contractAddress?.equals(contractAddress, ignoreCase = true) == true &&
                        networkId == network.networkId
                }
                if (tokenNetwork.isNotEmpty()) {
                    coin.copy(networks = tokenNetwork)
                } else {
                    null
                }
            }
            foundCoin?.let { FoundTokenConverter.convert(foundCoin) }
        }
    }

    override fun validateAddress(contractAddress: String, networkId: String): Boolean {
        return when (val blockchain = Blockchain.fromNetworkId(networkId) ?: Blockchain.Unknown) {
            Blockchain.Unknown, Blockchain.Binance, Blockchain.BinanceTestnet -> true
            else -> blockchain.validateAddress(contractAddress)
        }
    }
}