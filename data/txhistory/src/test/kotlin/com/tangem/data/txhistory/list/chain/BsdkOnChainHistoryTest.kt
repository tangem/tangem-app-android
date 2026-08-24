package com.tangem.data.txhistory.list.chain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryEnvironment
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryListBatchFlow
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.getEmittedValues
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.plus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Verifies the BSDK backbone: the pagination status maps to [HistoryState], the express overlay is merged in, and
 * [BsdkOnChainHistory.autoLoadMoreUntilScrollable] dispatches a `LoadMore` while the loaded list is too short to
 * scroll (fewer than the target) or the last page was empty, and stops otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BsdkOnChainHistoryTest {

    private val userWalletId = UserWalletId(stringValue = "01")
    private val rawCurrencyId = CryptoCurrency.RawID(value = "ethereum")
    private val currency = mockk<CryptoCurrency>(relaxed = true) {
        every { id.rawCurrencyId } returns rawCurrencyId
    }
    private val repository = mockk<TxHistoryRepositoryV2>()
    private val currencyUSDQuote = mockk<GetCurrencyUSDQuoteUseCase>()

    // region state mapping

    @Test
    fun `GIVEN initial loading WHEN loading THEN Loading`() = runTest {
        val states = collect(state(items = 0, status = PaginationStatus.InitialLoading))
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Loading)
    }

    @Test
    fun `GIVEN initial loading error WHEN loading THEN Error`() = runTest {
        val states = collect(state(items = 0, status = PaginationStatus.InitialLoadingError(RuntimeException("boom"))))
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Error)
    }

    @Test
    fun `GIVEN empty items reaching the end WHEN loading THEN Empty`() = runTest {
        val states = collect(state(items = 0, status = PaginationStatus.EndOfPagination))
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Empty)
    }

    @Test
    fun `GIVEN items and more to load WHEN loading THEN Content with hasMore true`() = runTest {
        val states = collect(state(items = 25, status = paginating(empty = false)))
        advanceUntilIdle()

        val last = states.last()
        assertThat(last).isInstanceOf(HistoryState.Content::class.java)
        with(last as HistoryState.Content) {
            assertThat(items).hasSize(25)
            assertThat(hasMore).isTrue()
            assertThat(isLoadingMore).isFalse()
        }
    }

    @Test
    fun `GIVEN items and end reached WHEN loading THEN Content with hasMore false`() = runTest {
        val states = collect(state(items = 25, status = PaginationStatus.EndOfPagination))
        advanceUntilIdle()

        assertThat((states.last() as HistoryState.Content).hasMore).isFalse()
    }

    @Test
    fun `GIVEN express op matched to a loaded on-chain tx WHEN loading THEN row is enriched`() = runTest {
        val states = collect(
            batchState = state(items = 25, status = PaginationStatus.EndOfPagination, firstTxHash = "match"),
            express = listOf(createSwap(matchHash = "match")),
        )
        advanceUntilIdle()

        val enriched = (states.last() as HistoryState.Content).items.filterIsInstance<ExpressTx.Swap>().single()
        assertThat(enriched.txInfo).isInstanceOf(OnChainTx.BSDK::class.java)
    }

    // endregion

    // region dust filter

    @Test
    fun `GIVEN a USD quote WHEN an incoming tx is worth less than a cent THEN it is hidden`() = runTest {
        // Arrange
        val dust = createDust(txHash = "dust") // $0.001
        val worthShowing = createTxInfo(txHash = "worth-showing", amount = BigDecimal.ONE, isOutgoing = false) // $1000

        // Act
        val states = collect(
            batchState = state(txs = listOf(dust, worthShowing), status = PaginationStatus.EndOfPagination),
            usdQuote = QUOTE,
        )
        advanceUntilIdle()

        // Assert
        val items = (states.last() as HistoryState.Content).items
        assertThat(items).containsExactly(OnChainTx.BSDK(worthShowing))
    }

    @Test
    fun `GIVEN a USD quote WHEN an incoming tx is worth exactly a cent THEN it is shown`() = runTest {
        // Arrange
        val onTheEdge = createTxInfo(txHash = "on-the-edge", amount = BigDecimal("0.00001"), isOutgoing = false)

        // Act
        val states = collect(
            batchState = state(txs = listOf(onTheEdge), status = PaginationStatus.EndOfPagination),
            usdQuote = QUOTE,
        )
        advanceUntilIdle()

        // Assert
        val items = (states.last() as HistoryState.Content).items
        assertThat(items).containsExactly(OnChainTx.BSDK(onTheEdge))
    }

    @Test
    fun `GIVEN a USD quote WHEN an outgoing tx is worth less than a cent THEN it is shown`() = runTest {
        // Arrange
        val tiny = createTxInfo(txHash = "tiny-send", amount = BigDecimal("0.000001"), isOutgoing = true)

        // Act
        val states = collect(
            batchState = state(txs = listOf(tiny), status = PaginationStatus.EndOfPagination),
            usdQuote = QUOTE,
        )
        advanceUntilIdle()

        // Assert
        val items = (states.last() as HistoryState.Content).items
        assertThat(items).containsExactly(OnChainTx.BSDK(tiny))
    }

    @Test
    fun `GIVEN a USD quote WHEN an approve carries no amount THEN it is shown`() = runTest {
        // Arrange
        val approve = createTxInfo(
            txHash = "approve",
            amount = BigDecimal.ZERO,
            isOutgoing = false,
            type = TxInfo.TransactionType.Approve(amount = null, address = "spender-addr"),
        )

        // Act
        val states = collect(
            batchState = state(txs = listOf(approve), status = PaginationStatus.EndOfPagination),
            usdQuote = QUOTE,
        )
        advanceUntilIdle()

        // Assert
        val items = (states.last() as HistoryState.Content).items
        assertThat(items).containsExactly(OnChainTx.BSDK(approve))
    }

    @Test
    fun `GIVEN no USD quote WHEN incoming txs are worth nothing THEN they are shown`() = runTest {
        // Arrange
        val txs = List(3) { createTxInfo(txHash = "hash-$it", amount = BigDecimal.ZERO, isOutgoing = false) }

        // Act
        val states = collect(state(txs = txs, status = PaginationStatus.EndOfPagination), usdQuote = null)
        advanceUntilIdle()

        // Assert
        assertThat((states.last() as HistoryState.Content).items).hasSize(3)
    }

    @Test
    fun `GIVEN a page long enough to scroll but mostly dust WHEN paginating THEN a LoadMore is dispatched`() = runTest {
        // Arrange
        val txs = List(20) { createDust(txHash = "dust-$it") } +
            List(5) { createTxInfo(txHash = "hash-$it", amount = BigDecimal.ONE) }

        // Act
        val actions = collectDispatchedActions(
            batchState = state(txs = txs, status = paginating(empty = false)),
            usdQuote = QUOTE,
        )
        advanceUntilIdle()

        // Assert
        assertThat(actions.filterIsInstance<BatchAction.LoadMore<*>>()).isNotEmpty()
    }

    // endregion

    // region auto-load

    @Test
    fun `GIVEN a short page with more to load WHEN paginating THEN a LoadMore is dispatched`() = runTest {
        val actions = collectDispatchedActions(state(items = 10, status = paginating(empty = false)))
        advanceUntilIdle()

        assertThat(actions.filterIsInstance<BatchAction.LoadMore<*>>()).isNotEmpty()
    }

    @Test
    fun `GIVEN a page long enough to scroll WHEN paginating THEN no LoadMore is dispatched`() = runTest {
        val actions = collectDispatchedActions(state(items = 25, status = paginating(empty = false)))
        advanceUntilIdle()

        assertThat(actions.filterIsInstance<BatchAction.LoadMore<*>>()).isEmpty()
    }

    @Test
    fun `GIVEN the last page came back empty WHEN paginating THEN a LoadMore is dispatched`() = runTest {
        // Enough items to be scrollable, but the last fetch was empty (a gap) → keep bridging to the end.
        val actions = collectDispatchedActions(state(items = 25, status = paginating(empty = true)))
        advanceUntilIdle()

        assertThat(actions.filterIsInstance<BatchAction.LoadMore<*>>()).isNotEmpty()
    }

    // endregion

    private fun TestScope.collect(
        batchState: BatchListState<Int, PaginationWrapper<TxInfo>>,
        express: List<ExpressTx> = emptyList(),
        usdQuote: BigDecimal? = null,
    ): List<HistoryState> {
        stubRepository(batchState, express)
        return getEmittedValues(createSut(usdQuote).history())
    }

    /**
     * Collects the [BatchAction]s the SUT dispatches to the pagination source, to assert the auto-load decision.
     *
     * The actions channel is rendezvous, so the source-side collector must be parked before the auto-load check
     * fires its `LoadMore`: we start collecting the captured `actionsFlow` (and let it park past the initial `Reload`)
     * before collecting `history()`, which is what triggers the auto-load check.
     */
    private fun TestScope.collectDispatchedActions(
        batchState: BatchListState<Int, PaginationWrapper<TxInfo>>,
        usdQuote: BigDecimal? = null,
    ): List<BatchAction<*, *, *>> {
        val contextSlot = slot<TxHistoryListBatchingContext>()
        stubRepository(batchState, express = emptyList(), contextSlot = contextSlot)

        val historyFlow = createSut(usdQuote).history() // captures the context synchronously
        val actions = getEmittedValues(contextSlot.captured.actionsFlow)
        advanceUntilIdle() // the source-side collector emits the initial Reload, then parks
        getEmittedValues(historyFlow) // triggers the auto-load check, which may dispatch a LoadMore
        return actions
    }

    private fun stubRepository(
        batchState: BatchListState<Int, PaginationWrapper<TxInfo>>,
        express: List<ExpressTx>,
        contextSlot: io.mockk.CapturingSlot<TxHistoryListBatchingContext>? = null,
    ) {
        val batchFlow = mockk<TxHistoryListBatchFlow> { every { state } returns MutableStateFlow(batchState) }
        if (contextSlot != null) {
            every { repository.getTxHistoryBatchFlow(any(), capture(contextSlot)) } returns batchFlow
        } else {
            every { repository.getTxHistoryBatchFlow(any(), any()) } returns batchFlow
        }
        every { repository.getExpressHistory(any(), any(), any()) } returns flowOf(express)
    }

    /**
     * The USD quote is loaded on the model scope, so that scope gets an unconfined dispatcher: `advanceUntilIdle()`
     * leaves background work on a [kotlinx.coroutines.test.StandardTestDispatcher] unrun, which would park the history
     * flow forever on the quote it awaits.
     */
    private fun TestScope.createSut(usdQuote: BigDecimal? = null): BsdkOnChainHistory {
        coEvery { currencyUSDQuote(rawCurrencyId) } returns usdQuote
        return BsdkOnChainHistory(
            repository = repository,
            currencyUSDQuote = currencyUSDQuote,
            env = HistoryEnvironment(
                userWalletId = userWalletId,
                currency = currency,
                modelScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
        )
    }

    private fun state(
        items: Int,
        status: PaginationStatus<PaginationWrapper<TxInfo>>,
        firstTxHash: String? = null,
        amount: BigDecimal = BigDecimal.ONE,
    ): BatchListState<Int, PaginationWrapper<TxInfo>> = state(
        txs = List(items) { index ->
            val hash = if (index == 0 && firstTxHash != null) firstTxHash else "hash-$index"
            createTxInfo(txHash = hash, amount = amount)
        },
        status = status,
    )

    private fun state(
        txs: List<TxInfo>,
        status: PaginationStatus<PaginationWrapper<TxInfo>>,
    ): BatchListState<Int, PaginationWrapper<TxInfo>> {
        val wrapper = PaginationWrapper(currentPage = Page.Initial, nextPage = Page.LastPage, items = txs)
        return BatchListState(
            data = if (txs.isEmpty()) emptyList() else listOf(Batch(key = 0, data = wrapper)),
            status = status,
        )
    }

    private fun paginating(empty: Boolean): PaginationStatus<PaginationWrapper<TxInfo>> {
        val wrapper = PaginationWrapper<TxInfo>(Page.Initial, Page.LastPage, items = emptyList())
        return PaginationStatus.Paginating(BatchFetchResult.Success(data = wrapper, empty = empty, last = false))
    }

    /** An incoming transfer worth $0.001 at [QUOTE] — under the cent the filter cuts at. */
    private fun createDust(txHash: String) =
        createTxInfo(txHash = txHash, amount = BigDecimal("0.000001"), isOutgoing = false)

    private fun createTxInfo(
        txHash: String,
        amount: BigDecimal = BigDecimal.ONE,
        isOutgoing: Boolean = true,
        type: TxInfo.TransactionType = TxInfo.TransactionType.Transfer,
    ) = TxInfo(
        txHash = txHash,
        timestampInMillis = 100,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User("addr")),
        sourceType = TxInfo.SourceType.Single("addr"),
        interactionAddressType = null,
        status = TxInfo.TransactionStatus.Confirmed,
        type = type,
        amount = amount,
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
            externalTxId = null,
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

    private companion object {
        val QUOTE: BigDecimal = BigDecimal("1000")
    }
}