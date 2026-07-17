package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListConfig
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.fetcher.CursorBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In MOCK env returns a canned transaction list covering every inline-cashback badge state;
 * otherwise delegates to [DefaultTangemPayTxHistoryRepository].
 */
@Singleton
internal class MockAwareTangemPayTxHistoryRepository @Inject constructor(
    private val real: DefaultTangemPayTxHistoryRepository,
    private val apiConfigsManager: ApiConfigsManager,
    private val dispatchers: CoroutineDispatcherProvider,
) : TangemPayTxHistoryRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(ApiConfig.ID.TangemPay)
            .environment == ApiEnvironment.MOCK

    override fun getTxHistoryBatchFlow(
        userWalletId: UserWalletId,
        batchSize: Int,
        context: TangemPayTxHistoryListBatchingContext,
    ): TangemPayTxHistoryListBatchFlow {
        if (!isMockMode) return real.getTxHistoryBatchFlow(userWalletId, batchSize, context)
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = mockFetcher(batchSize),
        ).toBatchFlow()
    }

    override suspend fun getTransaction(
        userWalletId: UserWalletId,
        transactionId: String,
    ): Either<VisaApiError, TangemPayTxHistoryItem?> {
        if (!isMockMode) return real.getTransaction(userWalletId, transactionId)
        return mockItems().firstOrNull { it.id == transactionId }.right()
    }

    private fun mockFetcher(
        batchSize: Int,
    ): BatchFetcher<TangemPayTxHistoryListConfig, List<TangemPayTxHistoryItem>> = CursorBatchFetcher(
        prefetchDistance = batchSize,
        batchSize = batchSize,
        subFetcher = { _, _, _ ->
            val items = mockItems()
            BatchFetchResult.Success(data = items, last = true, empty = items.isEmpty())
        },
        cursorFromItem = { it.id },
    )

    private fun mockItems(): List<TangemPayTxHistoryItem> {
        val now = DateTime.now()
        val yesterday = now.minusDays(1)
        return listOf(
            // Confirmed cashback — highlighted (blue) badge.
            mockSpend(
                id = "tx_1",
                merchantName = "Starbucks",
                category = "Restaurants",
                amount = BigDecimal("12.50"),
                date = now,
                cashback = mockCashback(Status.CONFIRMED, "0.63"),
            ),
            // Estimated cashback — neutral (grey) badge.
            mockSpend(
                id = "tx_2",
                merchantName = "Whole Foods",
                category = "Groceries",
                amount = BigDecimal("85.00"),
                date = now.minusHours(1),
                cashback = mockCashback(Status.ESTIMATED, "1.70"),
            ),
            // Refund — incoming amount with a negative (clawed-back) cashback.
            mockSpend(
                id = "tx_3",
                merchantName = "Amazon",
                category = "Shopping",
                amount = BigDecimal("-40.00"),
                date = now.minusHours(2),
                cashback = mockCashback(Status.CONFIRMED, "-0.80"),
            ),
            // Awaiting calculation — no badge (amount not yet known).
            mockSpend(
                id = "tx_4",
                merchantName = "Apple Store",
                category = "Electronics",
                amount = BigDecimal("1299.00"),
                date = now.minusHours(3),
                cashback = mockCashback(Status.AWAITING_CALCULATION, null),
            ),
            // Excluded — no cashback earned — no badge.
            mockSpend(
                id = "tx_5",
                merchantName = "Shell",
                category = "Gas",
                amount = BigDecimal("60.00"),
                date = yesterday,
                cashback = mockCashback(Status.EXCLUDED, "0.00"),
            ),
            // No cashback object at all — no badge.
            mockSpend(
                id = "tx_6",
                merchantName = "Netflix",
                category = "Entertainment",
                amount = BigDecimal("15.99"),
                date = yesterday.minusHours(1),
                cashback = null,
            ),
            // Non-spend transaction — no cashback.
            TangemPayTxHistoryItem.Payment(
                id = "tx_7",
                jsonRepresentation = "{}",
                date = yesterday.minusHours(2),
                amount = BigDecimal("100.00"),
                currency = USD,
                transactionHash = "0xmock",
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun mockSpend(
        id: String,
        merchantName: String,
        category: String,
        amount: BigDecimal,
        date: DateTime,
        cashback: TangemPayTxHistoryItem.Cashback?,
    ): TangemPayTxHistoryItem.Spend = TangemPayTxHistoryItem.Spend(
        id = id,
        jsonRepresentation = "{}",
        date = date,
        amount = amount,
        currency = USD,
        authorizedAmount = amount.abs(),
        localAmount = null,
        localCurrency = null,
        enrichedMerchantName = merchantName,
        merchantName = merchantName,
        enrichedMerchantCategory = category,
        merchantCategoryCode = null,
        merchantCategory = category,
        status = TangemPayTxHistoryItem.Status.COMPLETED,
        enrichedMerchantIconUrl = null,
        declinedReason = null,
        cardName = "Basic card",
        cardNumberLast4 = "9092",
        cashback = cashback,
    )

    private fun mockCashback(status: Status, amount: String?): TangemPayTxHistoryItem.Cashback {
        val value = amount?.let(::BigDecimal)
        return TangemPayTxHistoryItem.Cashback(
            status = status,
            amount = value,
            currency = value?.let { USD },
            isCapTrimmed = false,
            exclusionReason = null,
            promotionIds = emptyList(),
        )
    }

    private companion object {
        private val USD: Currency = Currency.getInstance("USD")
    }
}