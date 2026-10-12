package com.tangem.data.polymarket.pagination

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionsUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class PolymarketPositionsUpdateFetcherTest {

    private val config = PolymarketPositionsListConfig(mockk<PolymarketAddresses>(), PolymarketPositionsTab.ACTIVE)
    private val requestedLimits = mutableListOf<Int>()
    private val requestedOffsets = mutableListOf<Int>()

    @Test
    fun `GIVEN loaded pages WHEN refreshed THEN each is requested at its own offset with the page size`() = runTest {
        // Act
        refresh(
            pages = listOf(batch(key = 0, listOf(position("a"))), batch(key = 2, listOf(position("c")))),
            response = { emptyList() },
        )

        // Assert
        assertThat(requestedOffsets).containsExactly(0, 2 * BATCH_SIZE).inOrder()
        assertThat(requestedLimits).containsExactly(BATCH_SIZE, BATCH_SIZE)
    }

    @Test
    fun `GIVEN a holding gone from the response WHEN the page is refreshed THEN the page drops it`() = runTest {
        // Arrange
        val kept = position("a")
        val sold = position("b")

        // Act
        val context = refresh(pages = listOf(batch(key = 0, listOf(kept, sold))), response = { listOf(kept) })

        // Assert
        assertThat(context.positionsOf(key = 0)).containsExactly(kept)
    }

    @Test
    fun `GIVEN a holding only the response carries WHEN the page is refreshed THEN the page takes it in server order`() =
        runTest {
            // Arrange
            val existing = position("a")
            val bought = position("b")

            // Act
            val context = refresh(
                pages = listOf(batch(key = 0, listOf(existing))),
                response = { listOf(bought, existing) },
            )

            // Assert
            assertThat(context.positionsOf(key = 0)).containsExactly(bought, existing).inOrder()
        }

    @Test
    fun `GIVEN two loaded pages WHEN refreshed THEN each page takes only its own response`() = runTest {
        // Arrange
        val first = position("a")
        val second = position("b")

        // Act
        val context = refresh(
            pages = listOf(batch(key = 0, listOf(position("stale-a"))), batch(key = 1, listOf(position("stale-b")))),
            response = { offset -> if (offset == 0) listOf(first) else listOf(second) },
        )

        // Assert
        assertThat(context.positionsOf(key = 0)).containsExactly(first)
        assertThat(context.positionsOf(key = 1)).containsExactly(second)
    }

    @Test
    fun `GIVEN the first page fails WHEN refreshed THEN the failure propagates and later pages are not requested`() =
        runTest {
            // Arrange
            val stale = position("a")
            val fetcher = PolymarketPositionsUpdateFetcher(batchSize = BATCH_SIZE) { _, _, offset ->
                requestedOffsets += offset
                PolymarketPositionsError.Network.left()
            }
            val context = FakeUpdateContext(listOf(batch(key = 0, listOf(stale)), batch(key = 1, listOf(position("b")))))

            // Act
            val thrown = runCatching {
                with(fetcher) {
                    context.fetchUpdateAsync(
                        toUpdate = context.batches,
                        updateRequest = PolymarketPositionsUpdateRequest(config),
                    )
                }
            }.exceptionOrNull()

            // Assert
            assertThat(thrown).isInstanceOf(PolymarketPositionsReadException::class.java)
            assertThat((thrown as PolymarketPositionsReadException).error).isEqualTo(PolymarketPositionsError.Network)
            assertThat(requestedOffsets).containsExactly(0)
            assertThat(context.positionsOf(key = 0)).containsExactly(stale)
        }

    private suspend fun refresh(
        pages: List<Batch<Int, List<PolymarketPosition>>>,
        response: (offset: Int) -> List<PolymarketPosition>,
    ): FakeUpdateContext {
        val fetcher = PolymarketPositionsUpdateFetcher(batchSize = BATCH_SIZE) { _, limit, offset ->
            requestedLimits += limit
            requestedOffsets += offset
            PolymarketPositionsPage(response(offset), hasNext = true).right()
        }
        val context = FakeUpdateContext(pages)

        with(fetcher) {
            context.fetchUpdateAsync(toUpdate = context.batches, updateRequest = PolymarketPositionsUpdateRequest(config))
        }

        return context
    }

    private fun batch(key: Int, positions: List<PolymarketPosition>) = Batch(key = key, data = positions)

    private fun position(assetId: String): PolymarketPosition.Open = mockk {
        every { id } returns PolymarketPositionId(conditionId = "condition", assetId = assetId)
    }

    /** Applies updates the way [com.tangem.pagination.BatchListSource] does: by key, over the current batches. */
    private class FakeUpdateContext(
        batches: List<Batch<Int, List<PolymarketPosition>>>,
    ) : BatchUpdateFetcher.UpdateContext<Int, List<PolymarketPosition>> {

        var batches: List<Batch<Int, List<PolymarketPosition>>> = batches
            private set

        override suspend fun update(
            update: List<Batch<Int, List<PolymarketPosition>>>.() -> BatchUpdateResult<Int, List<PolymarketPosition>>,
        ) {
            val result = batches.update()
            if (result !is BatchUpdateResult.Success) return

            val updated = result.data.associateBy { it.key }
            batches = batches.map { updated[it.key] ?: it }
        }

        fun positionsOf(key: Int): List<PolymarketPosition> = batches.first { it.key == key }.data
    }

    private companion object {
        const val BATCH_SIZE = 30
    }
}