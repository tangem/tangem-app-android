package com.tangem.data.tokens.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * A PagingSource responsible for retrieving tokens from the Tangem Tech API and adding to them quotes information.
 *
 * @property api                          Tangem Tech API
 * @property quotesRepository             Repository providing quotes data.
 * @property dispatchers                  Coroutine dispatchers provider.
 * @property searchText                   The search text used to filter tokens.
 */
internal class CoinsPagingSource(
    private val api: TangemTechApi,
    private val quotesRepository: QuotesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val searchText: String?,
) : PagingSource<Int, Token>() {

    override fun getRefreshKey(state: PagingState<Int, Token>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(other = 1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(other = 1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Token> {
        val page = params.key ?: 0

        return com.tangem.utils.coroutines.runCatching(dispatchers.io) {
            api.getCoins(
                active = true, // TODO change when voting functionality is implemented
                searchText = searchText,
                offset = page * params.loadSize,
                limit = params.loadSize,
            ).getOrThrow()
        }.fold(
            onSuccess = { response ->
                val coinsIds = response.coins.map { coin ->
                    CryptoCurrency.ID(
                        prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                        body = CryptoCurrency.ID.Body.NetworkId(coin.id),
                        suffix = CryptoCurrency.ID.Suffix.RawID(coin.id),
                    )
                }

                try {
                    val quotes = quotesRepository.getQuotesSync(currenciesIds = coinsIds.toSet(), refresh = false)
                    LoadResult.Page(
                        data = CoinsResponseConverter.convert(
                            CoinsData(
                                response.coins,
                                response.imageHost,
                                quotes,
                            ),
                        ),
                        prevKey = if (page == 0) null else page.minus(other = 1),
                        nextKey = if (response.coins.isEmpty()) null else page.plus(other = 1),
                    )
                } catch (t: Throwable) {
                    LoadResult.Error(t)
                }
            },
            onFailure = { LoadResult.Error(it) },
        )
    }
}

data class CoinsData(
    val coins: List<CoinsResponse.Coin>,
    val imageHost: String?,
    val quotes: Set<Quote>,
)
