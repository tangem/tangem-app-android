package com.tangem.data.earn.repository.batch

import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.earn.converter.EarnTokenConverter
import com.tangem.data.earn.repository.DefaultEarnRepository.Companion.FIRST_PAGE
import com.tangem.data.earn.repository.createCryptoCurrencyForEarnToken
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow

internal class EarnTokensBatchFetcher(
    private val tangemTechApi: TangemTechApi,
    private val batchSize: Int,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val cryptoCurrencyFactory: CryptoCurrencyFactory,
) : BatchFetcher<EarnTokensListConfig, List<EarnTokenWithCurrency>> {

    private val state: MutableStateFlow<EarnTokensPaginationState?> = MutableStateFlow(null)

    override suspend fun fetchFirst(
        requestParams: EarnTokensListConfig,
    ): BatchFetchResult<List<EarnTokenWithCurrency>> {
        return runSuspendCatching {
            loadPage(
                page = FIRST_PAGE,
                params = requestParams,
                limit = batchSize,
            )
        }.fold(
            onSuccess = { result ->
                state.value = result.state
                result.batchResult
            },
            onFailure = { throwable -> BatchFetchResult.Error(throwable) },
        )
    }

    override suspend fun fetchNext(
        overrideRequestParams: EarnTokensListConfig?,
        lastResult: BatchFetchResult<List<EarnTokenWithCurrency>>,
    ): BatchFetchResult<List<EarnTokenWithCurrency>> {
        val currentState = state.value ?: return BatchFetchResult.Error(
            IllegalStateException("fetchFirst must be called"),
        )

        if (lastResult is BatchFetchResult.Success && lastResult.last && overrideRequestParams == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        val params = overrideRequestParams ?: currentState.params
        val shouldReset = overrideRequestParams != null && overrideRequestParams != currentState.params
        val pageToLoad = if (shouldReset) FIRST_PAGE else currentState.nextPage

        return runSuspendCatching {
            loadPage(
                page = pageToLoad,
                params = params,
                limit = batchSize,
            )
        }.fold(
            onSuccess = { result ->
                state.value = result.state
                result.batchResult
            },
            onFailure = { throwable -> BatchFetchResult.Error(throwable) },
        )
    }

    private suspend fun loadPage(page: Int, params: EarnTokensListConfig, limit: Int): PageLoadResult {
        fun createEarnTokenWithCurrency(userWallet: UserWallet, dto: EarnResponse): EarnTokenWithCurrency? {
            val earnToken = EarnTokenConverter.convert(dto)
            val cryptoCurrency = createCryptoCurrencyForEarnToken(
                cryptoCurrencyFactory = cryptoCurrencyFactory,
                userWallet = userWallet,
                earnToken = dto,
            )

            return cryptoCurrency?.let {
                EarnTokenWithCurrency(
                    earnToken = earnToken,
                    cryptoCurrency = cryptoCurrency,
                )
            }
        }

        val response = tangemTechApi.getEarnTokens(
            isForEarn = params.isForEarn,
            page = page.toString(),
            limit = limit,
            type = params.type,
            network = params.network,
        ).getOrThrow()

        val userWallet = userWalletsListRepository.selectedUserWallet.value

        val items = if (userWallet == null) {
            emptyList()
        } else {
            response.items.mapNotNull { dto ->
                createEarnTokenWithCurrency(userWallet, dto)
            }
        }

        val isLast = items.size < limit

        val batchResult = BatchFetchResult.Success(
            data = items,
            empty = items.isEmpty(),
            last = isLast,
        )

        return PageLoadResult(
            batchResult = batchResult,
            state = EarnTokensPaginationState(
                nextPage = response.meta.page + 1,
                params = params,
            ),
        )
    }

    private data class PageLoadResult(
        val batchResult: BatchFetchResult.Success<List<EarnTokenWithCurrency>>,
        val state: EarnTokensPaginationState,
    )

    private data class EarnTokensPaginationState(
        val nextPage: Int,
        val params: EarnTokensListConfig,
    )
}