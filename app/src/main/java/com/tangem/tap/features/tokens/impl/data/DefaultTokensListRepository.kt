package com.tangem.tap.features.tokens.impl.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.tap.features.tokens.impl.domain.TokensListRepository
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow

/**
 * Default repository implementation of tokens list feature
 *
 * @property tangemTechApi              Tangem Tech API
 * @property dispatchers                coroutine dispatchers provider
 * @property getSelectedWalletUseCase   use case that returns selected wallet
 * @property testnetTokensStorage       storage for getting testnet tokens data
 *
[REDACTED_AUTHOR]
 */
internal class DefaultTokensListRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
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
                val defaultSource = TangemApiTokensPagingSource(
                    api = tangemTechApi,
                    dispatchers = dispatchers,
                    getSelectedWalletUseCase = getSelectedWalletUseCase,
                    searchText = searchText,
                )

                getSelectedWalletUseCase().fold(
                    ifLeft = { defaultSource },
                    ifRight = {
                        if (it.scanResponse.card.isTestCard) {
                            TestnetTokensPagingSource(testnetTokensStorage, searchText)
                        } else {
                            defaultSource
                        }
                    },
                )
            },
        ).flow
    }
}