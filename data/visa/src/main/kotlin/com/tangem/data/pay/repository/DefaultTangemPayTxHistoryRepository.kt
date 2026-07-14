package com.tangem.data.pay.repository

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.utils.TangemPayTxHistoryItemConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayTxHistoryItemsStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListConfig
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.fetcher.CursorBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

private const val INITIAL_CURSOR = "initial_cursor_key"

internal class DefaultTangemPayTxHistoryRepository @Inject constructor(
    private val requestPerformer: TangemPayRequestPerformer,
    private val visaApi: TangemPayApi,
    private val cacheRegistry: CacheRegistry,
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore,
    private val dispatchers: CoroutineDispatcherProvider,
    @NetworkMoshi private val moshi: Moshi,
) : TangemPayTxHistoryRepository {

    private val txHistoryItemConverter by lazy { TangemPayTxHistoryItemConverter(moshi) }

    override fun getTxHistoryBatchFlow(
        userWalletId: UserWalletId,
        batchSize: Int,
        context: TangemPayTxHistoryListBatchingContext,
    ): TangemPayTxHistoryListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = createFetcher(userWalletId, batchSize),
        ).toBatchFlow()
    }

    private fun createFetcher(
        userWalletId: UserWalletId,
        batchSize: Int,
    ): BatchFetcher<TangemPayTxHistoryListConfig, List<TangemPayTxHistoryItem>> {
        return CursorBatchFetcher(
            prefetchDistance = batchSize,
            batchSize = batchSize,
            subFetcher = { request, _, _ ->
                val items = loadItems(
                    userWalletId = userWalletId,
                    config = request.params,
                    cursor = request.cursor,
                    limit = request.limit,
                )
                BatchFetchResult.Success(
                    data = items,
                    last = items.size < request.limit,
                    empty = items.isEmpty(),
                )
            },
            cursorFromItem = { item -> item.id }, // last item’s id becomes next cursor
        )
    }

    private suspend fun loadItems(
        userWalletId: UserWalletId,
        config: TangemPayTxHistoryListConfig,
        cursor: String?,
        limit: Int,
    ): List<TangemPayTxHistoryItem> {
        val storeKey = userWalletId.stringValue
        val storeCursor = cursor ?: INITIAL_CURSOR

        val fetchResult = runSuspendCatching {
            cacheRegistry.invokeOnExpire(
                key = getCacheKey(userWalletId = userWalletId, cursor = cursor),
                skipCache = config.shouldRefresh,
                block = { fetch(userWalletId = userWalletId, cursor = cursor, pageSize = limit) },
            )
        }
        val storedPage = txHistoryItemsStore.getSyncOrNull(key = storeKey, cursor = storeCursor)

        return fetchResult.fold(
            onSuccess = { storedPage.orEmpty() },
            onFailure = { e -> storedPage?.takeIf { it.isNotEmpty() } ?: throw e },
        )
    }

    private fun getCacheKey(userWalletId: UserWalletId, cursor: String?): String {
        return "tangem_pay_tx_history_${userWalletId.stringValue}_${cursor ?: INITIAL_CURSOR}"
    }

    override suspend fun getTransaction(
        userWalletId: UserWalletId,
        transactionId: String,
    ): Either<VisaApiError, TangemPayTxHistoryItem?> {
        return requestPerformer.performRequest(userWalletId = userWalletId) { authHeader ->
            visaApi.getCustomerTransaction(authHeader = authHeader, transactionId = transactionId)
        }.map { response ->
            txHistoryItemConverter.convert(response.result)
        }
    }

    private suspend fun fetch(userWalletId: UserWalletId, cursor: String?, pageSize: Int) {
        requestPerformer.performRequest(userWalletId = userWalletId) { authHeader ->
            visaApi.getTangemPayTxHistory(authHeader = authHeader, limit = pageSize, cursor = cursor)
        }.onLeft {
            error(it.toString())
        }.onRight { response ->
            val result = response.result
            val items = txHistoryItemConverter.convertList(result.transactions).filterNotNull()
            txHistoryItemsStore.store(key = userWalletId.stringValue, cursor = cursor ?: INITIAL_CURSOR, value = items)
        }
    }
}