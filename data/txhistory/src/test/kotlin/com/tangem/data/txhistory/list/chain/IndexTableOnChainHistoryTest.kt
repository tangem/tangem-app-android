package com.tangem.data.txhistory.list.chain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryEnvironment
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.repository.ExpressHistoryPage
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Verifies the index-backed express backbone (used when a currency has no on-chain history source): it maps an
 * [ExpressHistoryPage] to [HistoryState] and grows/reset the paging window on `LoadMore` / `Reload`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IndexTableOnChainHistoryTest {

    private val userWalletId = UserWalletId(stringValue = "01")
    private val currency = mockk<CryptoCurrency>(relaxed = true)
    private val repository = mockk<TxHistoryRepositoryV2>()

    @BeforeEach
    fun setup() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN empty page WHEN loading THEN Empty`() = runTest {
        every { repository.getIndexedExpressHistory(any(), any(), any()) } returns
            flowOf(ExpressHistoryPage(items = emptyList(), hasMore = false))

        val states = getEmittedValues(createSut().history())
        advanceUntilIdle()

        assertThat(states.last()).isEqualTo(HistoryState.Empty)
    }

    @Test
    fun `GIVEN non-empty page with more WHEN loading THEN Content with hasMore true`() = runTest {
        every { repository.getIndexedExpressHistory(any(), any(), any()) } returns
            flowOf(ExpressHistoryPage(items = listOf(createSwap()), hasMore = true))

        val states = getEmittedValues(createSut().history())
        advanceUntilIdle()

        val last = states.last()
        assertThat(last).isInstanceOf(HistoryState.Content::class.java)
        with(last as HistoryState.Content) {
            assertThat(items).hasSize(1)
            assertThat(hasMore).isTrue()
            assertThat(isLoadingMore).isFalse()
        }
    }

    @Test
    fun `GIVEN loaded page WHEN loadMore THEN window grows by page size`() = runTest {
        every { repository.getIndexedExpressHistory(any(), any(), any()) } returns
            flowOf(ExpressHistoryPage(items = listOf(createSwap()), hasMore = true))

        val sut = createSut()
        getEmittedValues(sut.history())
        advanceUntilIdle()

        sut.sendAction(Action.LoadMore)
        advanceUntilIdle()

        verify { repository.getIndexedExpressHistory(userWalletId, currency, PAGE_SIZE) }
        verify { repository.getIndexedExpressHistory(userWalletId, currency, PAGE_SIZE * 2) }
    }

    @Test
    fun `GIVEN grown window WHEN reload THEN window resets to page size`() = runTest {
        every { repository.getIndexedExpressHistory(any(), any(), any()) } returns
            flowOf(ExpressHistoryPage(items = listOf(createSwap()), hasMore = true))

        val sut = createSut()
        getEmittedValues(sut.history())
        advanceUntilIdle()
        sut.sendAction(Action.LoadMore)
        advanceUntilIdle()
        clearMocks(repository, answers = false)

        sut.sendAction(Action.Reload(shouldRefresh = true))
        advanceUntilIdle()

        verify { repository.getIndexedExpressHistory(userWalletId, currency, PAGE_SIZE) }
    }

    private fun TestScope.createSut() = IndexTableOnChainHistory(
        repository = repository,
        env = HistoryEnvironment(userWalletId = userWalletId, currency = currency, modelScope = backgroundScope),
    )

    private fun createSwap() = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "tx-1",
            status = ExpressExchangeStatus.Waiting,
            createdAtMillis = 100,
            provider = null,
            payinHash = null,
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
        const val PAGE_SIZE = 50
    }
}