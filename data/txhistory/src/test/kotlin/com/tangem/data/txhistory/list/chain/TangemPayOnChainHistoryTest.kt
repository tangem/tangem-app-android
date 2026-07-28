package com.tangem.data.txhistory.list.chain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryEnvironment
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.getEmittedValues
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Currency

/**
 * Verifies the TangemPay backbone: items map to [OnChainTx.TangemPay], the pagination status maps to [HistoryState],
 * and the express overlay is merged in by hash.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayOnChainHistoryTest {

    private val userWalletId = UserWalletId(stringValue = "01")
    private val currency = mockk<CryptoCurrency>(relaxed = true)
    private val repository = mockk<TangemPayTxHistoryRepository>()
    private val txHistoryRepository = mockk<TxHistoryRepositoryV2>()

    @Test
    fun `GIVEN loaded items and end WHEN loading THEN Content of TangemPay rows`() = runTest {
        val payment = createPayment(id = "p1", transactionHash = "h1")
        val states = collect(
            batchState = batchListState(items = listOf(payment), status = PaginationStatus.EndOfPagination),
            express = emptyList(),
        )
        advanceUntilIdle()

        val last = states.last()
        assertThat(last).isInstanceOf(HistoryState.Content::class.java)
        with(last as HistoryState.Content) {
            assertThat(hasMore).isFalse()
            assertThat(items.single()).isInstanceOf(OnChainTx.TangemPay::class.java)
        }
    }

    @Test
    fun `GIVEN initial loading WHEN loading THEN Loading`() = runTest {
        val states = collect(
            batchState = batchListState(items = emptyList(), status = PaginationStatus.InitialLoading),
            express = emptyList(),
        )
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Loading)
    }

    @Test
    fun `GIVEN initial loading error WHEN loading THEN Error`() = runTest {
        val states = collect(
            batchState = batchListState(
                items = emptyList(),
                status = PaginationStatus.InitialLoadingError(throwable = RuntimeException("boom")),
            ),
            express = emptyList(),
        )
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Error)
    }

    @Test
    fun `GIVEN empty items reaching the end WHEN loading THEN Empty`() = runTest {
        val states = collect(
            batchState = batchListState(items = emptyList(), status = PaginationStatus.EndOfPagination),
            express = emptyList(),
        )
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Empty)
    }

    @Test
    fun `GIVEN express op matching a TangemPay hash WHEN loading THEN row is enriched`() = runTest {
        val payment = createPayment(id = "p1", transactionHash = "match")
        val express = listOf(createSwap(matchHash = "match"))
        val states = collect(
            batchState = batchListState(items = listOf(payment), status = PaginationStatus.EndOfPagination),
            express = express,
        )
        advanceUntilIdle()

        val items = (states.last() as HistoryState.Content).items
        val enriched = items.filterIsInstance<ExpressTx.Swap>().single()
        assertThat(enriched.txInfo).isInstanceOf(OnChainTx.TangemPay::class.java)
    }

    private fun TestScope.collect(
        batchState: BatchListState<Int, List<TangemPayTxHistoryItem>>,
        express: List<ExpressTx>,
    ): List<HistoryState> {
        val batchFlow = mockk<TangemPayTxHistoryListBatchFlow> {
            every { state } returns MutableStateFlow(batchState)
        }
        every { repository.getTxHistoryBatchFlow(any(), any(), any()) } returns batchFlow
        every { txHistoryRepository.getExpressHistory(any(), any(), any()) } returns flowOf(express)

        val sut = TangemPayOnChainHistory(
            repository = repository,
            txHistoryRepository = txHistoryRepository,
            env = HistoryEnvironment(userWalletId = userWalletId, currency = currency, modelScope = backgroundScope),
        )
        return getEmittedValues(sut.history())
    }

    private fun batchListState(
        items: List<TangemPayTxHistoryItem>,
        status: PaginationStatus<List<TangemPayTxHistoryItem>>,
    ) = BatchListState(
        data = if (items.isEmpty()) emptyList() else listOf(Batch(key = 0, data = items)),
        status = status,
    )

    private fun createPayment(id: String, transactionHash: String) = TangemPayTxHistoryItem.Payment(
        id = id,
        jsonRepresentation = "",
        date = DateTime(100L),
        amount = BigDecimal.ONE,
        currency = Currency.getInstance("USD"),
        transactionHash = transactionHash,
    )

    private fun createSwap(matchHash: String) = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "tx-1",
            status = ExpressExchangeStatus.Finished,
            createdAtMillis = 100,
            provider = null,
            payinHash = matchHash,
            payoutHash = null,
            fromAddress = "from-addr",
            payoutAddress = "payout-addr",
            fromAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "eth", contractAddress = "0"),
                amount = BigDecimal.ONE,
                decimals = 18,
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "btc", contractAddress = "0xt"),
                amount = BigDecimal.ONE,
                decimals = 8,
            ),
            externalTxUrl = null,
            payinAddress = "payin-addr",
            updatedAtMillis = 100,
            refundAssetId = null,
            refundCurrency = null,
            fromAmount = BigDecimal.ONE,
            toAmount = BigDecimal.ONE,
            toActualAmount = null,
        ),
        isOutgoing = true,
        txInfo = null,
    )
}