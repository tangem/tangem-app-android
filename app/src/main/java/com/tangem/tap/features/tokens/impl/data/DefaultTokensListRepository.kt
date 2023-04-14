package com.tangem.tap.features.tokens.impl.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.tap.features.tokens.impl.domain.TokensListRepository
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow

/**
 * Default repository implementation of tokens list feature
 *
 * @property tangemTechApi        Tangem Tech API
 * @property dispatchers          coroutine dispatchers provider
 * @property reduxStateHolder     redux state holder
 * @property testnetTokensStorage storage for getting testnet tokens data
 *
 * @author Andrew Khokhlov on 07/04/2023
 */
internal class DefaultTokensListRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: AppStateHolder,
    private val testnetTokensStorage: TestnetTokensStorage,
) : TokensListRepository {

    override fun getAvailableTokens(searchText: String?): Flow<PagingData<Token>> {
        return Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 70,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                if (reduxStateHolder.scanResponse?.card?.isTestCard == true) {
                    TestnetTokensPagingSource(testnetTokensStorage, searchText)
                } else {
                    TangemApiTokensPagingSource(
                        api = tangemTechApi,
                        dispatchers = dispatchers,
                        reduxStateHolder = reduxStateHolder,
                        searchText = searchText,
                    )
                }
            },
        ).flow
    }
}
