package com.tangem.data.visa.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.lib.visa.api.VisaApi
import com.tangem.lib.visa.model.VisaTxHistoryResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class VisaTxHistoryPagingSource(
    params: Params,
    private val cacheRegistry: CacheRegistry,
    private val fetchedItems: MutableStateFlow<Map<String, List<VisaTxHistoryResponse.Transaction>>>,
    private val dispatchers: CoroutineDispatcherProvider,
    val requestTxHistory: suspend (offset: Int, pageSize: Int) -> VisaTxHistoryResponse,
) : PagingSource<Int, VisaTxHistoryItem>() {

    private val itemsFactory = VisaTxHistoryItemFactory()

    private val cardPublicKey = params.cardPublicKey
    private val pageSize = params.pageSize
    private val isRefresh = params.isRefresh

    private val pagedItems = MutableStateFlow<Map<Int, List<VisaTxHistoryItem>>>(
        value = emptyMap(),
    )

    override fun getRefreshKey(state: PagingState<Int, VisaTxHistoryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)

            anchorPage?.prevKey?.plus(pageSize) ?: anchorPage?.nextKey?.minus(pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VisaTxHistoryItem> {
        val offsetToLoad = params.key ?: INITIAL_OFFSET

        return try {
            fetchItemsIfExpired(offsetToLoad, pageSize, isRefresh = isRefresh && params is LoadParams.Refresh)

            val items = pagedItems.value[offsetToLoad].orEmpty()
            val prevOffset = when {
                items.isEmpty() -> null
                offsetToLoad > INITIAL_OFFSET -> offsetToLoad - pageSize
                else -> null
            }
            val nextOffset = when {
                items.isEmpty() -> INITIAL_OFFSET
                items.size % pageSize == 0 -> offsetToLoad + pageSize
                else -> null
            }

            LoadResult.Page(items, prevOffset, nextOffset)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load the transaction history for the requested offset: $offsetToLoad")
            LoadResult.Error(e)
        }
    }

    private suspend fun fetchItemsIfExpired(offset: Int, pageSize: Int, isRefresh: Boolean) {
        cacheRegistry.invokeOnExpire(
            key = getCacheKey(offset),
            skipCache = isRefresh,
            block = { fetchItems(offset, pageSize) },
        )
    }

    private suspend fun fetchItems(offset: Int, pageSize: Int) = withContext(dispatchers.io) {
        val response = requestTxHistory(offset, pageSize)

        fetchedItems.update {
            it.toMutableMap().apply {
                this[cardPublicKey] = this[cardPublicKey].orEmpty() + response.transactions
            }
        }

        pagedItems.update {
            it.toMutableMap().apply {
                this[offset] = response.transactions.map(itemsFactory::create)
            }
        }
    }

    private fun getCacheKey(offset: Int): String {
        return "visa_tx_history_${cardPublicKey}_$offset"
    }

    class Params(
        val userWallet: UserWallet,
        val cardPublicKey: String,
        val pageSize: Int,
        val isRefresh: Boolean,
    )

    private companion object {
        const val INITIAL_OFFSET = 0
    }
}