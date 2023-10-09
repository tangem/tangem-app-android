package com.tangem.tap.features.tokens.impl.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.tap.features.tokens.impl.data.converters.CoinsResponseConverter
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching

/**
 * Paging source that get tokens by Tangem Tech API
 *
 * @property api                        Tangem Tech API
 * @property dispatchers                coroutine dispatchers provider
 * @property getSelectedWalletUseCase   use case that returns selected wallet
 * @property searchText                 search text
 */
internal class TangemApiTokensPagingSource(
    private val api: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
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

        return runCatching(dispatchers.io) {
            val supportedBlockchains = getSelectedWalletUseCase().fold(
                ifLeft = { Blockchain.values().toList() },
                ifRight = { it.scanResponse.card.supportedBlockchains(it.scanResponse.cardTypesResolver) },
            )

            api.getCoins(
                networkIds = supportedBlockchains.joinToString(separator = ",", transform = Blockchain::toNetworkId),
                active = true,
                searchText = searchText,
                offset = page * params.loadSize,
                limit = params.loadSize,
            )
        }.fold(
            onSuccess = { response ->
                LoadResult.Page(
                    data = CoinsResponseConverter.convert(response),
                    prevKey = if (page == 0) null else page.minus(other = 1),
                    nextKey = if (response.coins.isEmpty()) null else page.plus(other = 1),
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }
}
