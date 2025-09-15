package com.tangem.data.pay.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.utils.VisaApiRequestMaker
import com.tangem.data.visa.utils.VisaTxHistoryItemConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.VisaTxHistoryResponse
import com.tangem.datasource.local.visa.TangemPayTxHistoryItemsStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListConfig
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import org.joda.time.DateTime
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("UnusedPrivateMember", "MagicNumber", "UnnecessaryParentheses")
internal class DefaultTangemPayTxHistoryRepository @Inject constructor(
    private val visaApiRequestMaker: VisaApiRequestMaker,
    private val visaApi: TangemPayApi,
    private val cacheRegistry: CacheRegistry,
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : TangemPayTxHistoryRepository {

    override fun getTxHistoryBatchFlow(
        batchSize: Int,
        context: TangemPayTxHistoryListBatchingContext,
    ): TangemPayTxHistoryListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = createFetcher(batchSize),
        ).toBatchFlow()
    }

    private fun createFetcher(
        batchSize: Int,
    ): LimitOffsetBatchFetcher<TangemPayTxHistoryListConfig, List<VisaTxHistoryItem>> {
        return LimitOffsetBatchFetcher(
            prefetchDistance = batchSize,
            batchSize = batchSize,
            subFetcher = { config, _, isInitialLoading ->
                val items = loadItems(config = config.params, offset = config.offset, limit = config.limit)
                BatchFetchResult.Success(
                    data = items,
                    last = items.size < batchSize,
                    empty = items.isEmpty(),
                )
            },
        )
    }

    private suspend fun loadItems(
        config: TangemPayTxHistoryListConfig,
        offset: Int,
        limit: Int,
    ): List<VisaTxHistoryItem> {
        cacheRegistry.invokeOnExpire(
            key = getCacheKey(userWalletId = config.userWalletId, offset = offset),
            skipCache = config.refresh,
            // TODO: TangemPay uncomment while BFF will be ready
            // block = { fetch(userWalletId = config.userWalletId, offset = offset, pageSize = limit) },
            block = { fetchMocked(userWalletId = config.userWalletId) },
        )

        return txHistoryItemsStore.getSyncOrNull(key = config.userWalletId, offset = offset).orEmpty()
    }

    private fun getCacheKey(userWalletId: UserWalletId, offset: Int): String {
        return "tangem_pay_tx_history_${userWalletId}_$offset"
    }

    private suspend fun fetch(userWalletId: UserWalletId, offset: Int, pageSize: Int) {
        val response = visaApiRequestMaker.request(userWalletId = userWalletId) { authHeader, accessCodeData ->
            visaApi.getTxHistory(
                authHeader = authHeader,
                customerId = accessCodeData.customerId,
                productInstanceId = accessCodeData.productInstanceId,
                limit = pageSize,
                offset = offset,
            )
        }
        val items = VisaTxHistoryItemConverter.convertList(response.transactions)

        txHistoryItemsStore.store(key = userWalletId, offset = offset, value = items)
    }

    private suspend fun fetchMocked(userWalletId: UserWalletId) {
        delay(2000) // initiating network request
        val items = VisaTxHistoryItemConverter.convertList(MOCKED_RESPONSE.transactions)
        txHistoryItemsStore.store(key = userWalletId, offset = 0, value = items)
    }

    companion object {
        private val now = DateTime.now()
        private var transactionIdCounter = 1000L

        private val todayTransactions = List(2) { index ->
            VisaTxHistoryResponse.Transaction(
                transactionId = transactionIdCounter++,
                transactionDt = now.minusHours(index + 1).minusMinutes(index * 15),
                transactionStatus = "Completed", // As "Success" might map to "Completed"
                transactionType = "Purchase",
                billingAmount = BigDecimal("${10 + index * 5}.${20 + index * 3}"),
                billingCurrencyCode = 840, // USD
                transactionAmount = BigDecimal("${10 + index * 5}.${20 + index * 3}"),
                transactionCurrencyCode = 840, // USD
                merchantName = "Online Store ${'A' + index}",
                merchantCity = "San Francisco",
                merchantCountryCode = "US",
                merchantCategoryCode = "5411", // Grocery Stores
                authCode = "AUTH${12345 + index}",
                rrn = "RRN00${100 + index}",
                blockchainAmount = BigDecimal("0.001").multiply(BigDecimal(index + 1)),
                blockchainCoinName = "ETH",
                blockchainFee = BigDecimal("0.00005"),
                requests = emptyList(),
            )
        }

        private val yesterdayTransactions = List(4) { index ->
            VisaTxHistoryResponse.Transaction(
                transactionId = transactionIdCounter++,
                transactionDt = now.minusDays(1).withTime(10 + index * 2, 15 * index % 60, index * 5 % 60, 0),
                transactionStatus = "Completed",
                transactionType = "Purchase",
                billingAmount = BigDecimal("${20 + index * 7}.${10 + index * 2}"),
                billingCurrencyCode = 840, // USD
                transactionAmount = BigDecimal("${20 + index * 7}.${10 + index * 2}"),
                transactionCurrencyCode = 840, // USD
                merchantName = "Coffee Shop ${'X' + index}",
                merchantCity = "Berlin",
                merchantCountryCode = "DE",
                merchantCategoryCode = "5812", // Restaurants
                authCode = "AUTH${22345 + index}",
                rrn = "RRN00${200 + index}",
                blockchainAmount = BigDecimal("0.001").multiply(BigDecimal(index + 1)),
                blockchainCoinName = "ETH",
                blockchainFee = BigDecimal("0.00005"),
                requests = emptyList(),
            )
        }

        private val threeDaysAgoTransactions = List(10) { index ->
            VisaTxHistoryResponse.Transaction(
                transactionId = transactionIdCounter++,
                transactionDt = now.minusDays(3).withTime(9 + index, (index * 10) % 60, (index * 20) % 60, 0),
                transactionStatus = "Completed",
                transactionType = "Purchase",
                billingAmount = BigDecimal("${5 + index * 2}.${50 + index}"),
                billingCurrencyCode = 840, // USD
                transactionAmount = BigDecimal("${5 + index * 2}.${50 + index}"),
                transactionCurrencyCode = 840, // USD
                merchantName = "Gadget Store ${'M' + index}",
                merchantCity = if (index % 2 == 0) "London" else "Paris",
                merchantCountryCode = if (index % 2 == 0) "GB" else "FR",
                merchantCategoryCode = "5732", // Electronic Sales
                authCode = "AUTH${32345 + index}",
                rrn = "RRN00${300 + index}",
                blockchainAmount = BigDecimal("0.001").multiply(BigDecimal(index + 1)),
                blockchainCoinName = "ETH",
                blockchainFee = BigDecimal("0.00005"),
                requests = emptyList(),
            )
        }
        private val MOCKED_RESPONSE = VisaTxHistoryResponse(
            cardWalletAddress = "0xYourVisaVirtualCardAddressHere",
            transactions = todayTransactions + yesterdayTransactions + threeDaysAgoTransactions,
        )
    }
}