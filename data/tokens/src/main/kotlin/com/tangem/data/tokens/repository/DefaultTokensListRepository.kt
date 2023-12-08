package com.tangem.data.tokens.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.data.tokens.paging.CoinsPagingSource
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensListRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow

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
}
