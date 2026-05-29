package com.tangem.features.txhistory.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.entity.TxHistoryItemsUM.TxHistoryItemUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TxHistoryUiManagerTest {

    private val state = MutableStateFlow(TxHistoryListState(status = paginatingStatus()))
    private val converter = mockk<TxHistoryItemToTransactionItemUMConverter>()
    private lateinit var manager: TxHistoryUiManager

    @BeforeEach
    fun setup() {
        state.value = TxHistoryListState(status = paginatingStatus())
        every { converter.convert(any()) } answers {
            mockk<TransactionItemUM>(relaxed = true)
        }
        manager = TxHistoryUiManager(state)
    }

    @Test
    fun `single batch with same date produces one group title`() {
        val batch = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
            txAt(millis("2025-10-08 20:00")),
            txAt(millis("2025-10-08 18:00")),
        )

        val result = update(listOf(batch), clear = true)

        val titles = result.groupTitles()
        assertThat(titles).hasSize(1)
    }

    @Test
    fun `single batch with different dates produces group title per date`() {
        val batch = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
            txAt(millis("2025-10-07 18:00")),
            txAt(millis("2025-10-06 10:00")),
        )

        val result = update(listOf(batch), clear = true)

        val titles = result.groupTitles()
        assertThat(titles).hasSize(3)
        assertThat(titles.distinct()).hasSize(3)
    }

    @Test
    fun `second batch with same date as first does not duplicate group title`() {
        val batch0 = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
            txAt(millis("2025-10-08 20:00")),
        )
        val batch1 = batchOf(
            key = 1,
            txAt(millis("2025-10-08 18:00")),
            txAt(millis("2025-10-08 16:00")),
        )

        val afterFirst = update(listOf(batch0), clear = true)
        state.value = state.value.copy(uiBatches = afterFirst)

        val afterSecond = update(listOf(batch0, batch1))

        val allTitles = afterSecond.groupTitles()
        assertThat(allTitles).hasSize(1)
    }

    @Test
    fun `second batch with different date adds new group title`() {
        val batch0 = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
            txAt(millis("2025-10-08 20:00")),
        )
        val batch1 = batchOf(
            key = 1,
            txAt(millis("2025-10-07 18:00")),
            txAt(millis("2025-10-07 16:00")),
        )

        val afterFirst = update(listOf(batch0), clear = true)
        state.value = state.value.copy(uiBatches = afterFirst)

        val afterSecond = update(listOf(batch0, batch1))

        val allTitles = afterSecond.groupTitles()
        assertThat(allTitles).hasSize(2)
        assertThat(allTitles.distinct()).hasSize(2)
    }

    @Test
    fun `second batch splits across date boundary`() {
        val batch0 = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
            txAt(millis("2025-10-08 20:00")),
        )
        val batch1 = batchOf(
            key = 1,
            txAt(millis("2025-10-08 16:00")),
            txAt(millis("2025-10-07 23:00")),
            txAt(millis("2025-10-07 22:00")),
        )

        val afterFirst = update(listOf(batch0), clear = true)
        state.value = state.value.copy(uiBatches = afterFirst)

        val afterSecond = update(listOf(batch0, batch1))

        val allTitles = afterSecond.groupTitles()
        // One title for Oct 8 (from batch0, continued in batch1), one for Oct 7
        assertThat(allTitles).hasSize(2)
    }

    @Test
    fun `third batch with same date as previous batches does not duplicate group title`() {
        val batch0 = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
            txAt(millis("2025-10-08 20:00")),
        )
        val batch1 = batchOf(
            key = 1,
            txAt(millis("2025-10-08 18:00")),
            txAt(millis("2025-10-08 16:00")),
        )
        val batch2 = batchOf(
            key = 2,
            txAt(millis("2025-10-08 14:00")),
            txAt(millis("2025-10-08 12:00")),
        )

        val afterThreeBatches = update(listOf(batch0, batch1, batch2), clear = true)

        val allTitles = afterThreeBatches.groupTitles()
        assertThat(allTitles).hasSize(1)
    }

    @Test
    fun `clearUiBatches resets grouping context`() {
        val batch0 = batchOf(
            key = 0,
            txAt(millis("2025-10-08 22:00")),
        )

        val afterFirst = update(listOf(batch0), clear = true)
        state.value = state.value.copy(uiBatches = afterFirst)

        // Same date but cleared — should get a fresh title
        val batch0New = batchOf(
            key = 0,
            txAt(millis("2025-10-08 18:00")),
        )
        val afterClear = update(listOf(batch0New), clear = true)

        val titles = afterClear.groupTitles()
        assertThat(titles).hasSize(1)
    }

    // --- Helpers ---

    private fun update(
        batches: List<Batch<Int, PaginationWrapper<TxInfo>>>,
        clear: Boolean = false,
    ) = manager.createOrUpdateUiBatches(
        newCurrencyBatches = batches,
        shouldClearUiBatches = clear,
        converter = converter,
    )

    private fun txAt(timestampMillis: Long): TxInfo = TxInfo(
        txHash = "hash_$timestampMillis",
        timestampInMillis = timestampMillis,
        isOutgoing = true,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User("addr")),
        sourceType = TxInfo.SourceType.Single("addr"),
        interactionAddressType = TxInfo.InteractionAddressType.User("addr"),
        status = TxInfo.TransactionStatus.Confirmed,
        type = TxInfo.TransactionType.Transfer,
        amount = BigDecimal.ONE,
    )

    private fun batchOf(key: Int, vararg items: TxInfo) = Batch(
        key = key,
        data = PaginationWrapper(
            currentPage = Page.Initial,
            nextPage = Page.Next((key + 1).toString()),
            items = items.toList(),
        ),
    )

    private fun millis(dateTime: String): Long {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        return requireNotNull(format.parse(dateTime)).time
    }

    private fun List<Batch<Int, List<TxHistoryItemUM>>>.groupTitles(): List<String> {
        return flatMap { it.data }
            .filterIsInstance<TxHistoryItemUM.GroupTitle>()
            .map { it.title }
    }

    private fun paginatingStatus() = PaginationStatus.Paginating(
        lastResult = BatchFetchResult.Success(data = Unit, empty = false, last = false),
    )
}