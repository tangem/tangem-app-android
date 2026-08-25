package com.tangem.data.polymarket.pagination

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsPage
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

internal class PolymarketEventsUpdateFetcherTest {

    private val config = PolymarketEventsListConfig(category = 7)

    private val requestedCursors = mutableListOf<String?>()
    private val requestedLimits = mutableListOf<Int>()

    @Test
    fun `GIVEN an event served again WHEN the page is refreshed THEN its data is replaced in place`() = runTest {
        // Arrange
        val stale = event(id = "event-1")
        val fresh = event(id = "event-1")
        val context = refresh(page = batch(key = 0, events = listOf(stale)), response = listOf(fresh))

        // Assert
        assertThat(context.eventsOf(key = 0)).containsExactly(fresh)
    }

    @Test
    fun `GIVEN an event the response no longer carries WHEN the page is refreshed THEN it keeps its old data`() =
        runTest {
            // Arrange
            val staleFirst = event(id = "event-1")
            val staleSecond = event(id = "event-2")
            val freshFirst = event(id = "event-1")

            // Act
            val context = refresh(
                page = batch(key = 0, events = listOf(staleFirst, staleSecond)),
                response = listOf(freshFirst),
            )

            // Assert
            assertThat(context.eventsOf(key = 0)).containsExactly(freshFirst, staleSecond).inOrder()
        }

    @Test
    fun `GIVEN an event only the response carries WHEN the page is refreshed THEN the page does not take it`() =
        runTest {
            // Arrange
            val stale = event(id = "event-1")
            val drifted = event(id = "event-2")

            // Act
            val context = refresh(page = batch(key = 0, events = listOf(stale)), response = listOf(stale, drifted))

            // Assert
            assertThat(context.eventsOf(key = 0)).containsExactly(stale)
        }

    @Test
    fun `GIVEN a loaded page WHEN it is refreshed THEN it is requested by the cursor it was loaded with`() = runTest {
        // Act
        refresh(
            page = batch(key = 1, events = listOf(event(id = "event-1")), cursor = "cursor-0"),
            response = emptyList(),
        )

        // Assert
        assertThat(requestedCursors).containsExactly("cursor-0")
        assertThat(requestedLimits).containsExactly(BATCH_SIZE)
    }

    @Test
    fun `GIVEN the refresh request fails WHEN the page is refreshed THEN the failure is not swallowed`() = runTest {
        // Arrange
        val fetcher = PolymarketEventsUpdateFetcher(batchSize = BATCH_SIZE, fetchPage = { _, _, _ -> throw IOException() })
        val context = FakeUpdateContext(batches = listOf(batch(key = 0, events = listOf(event(id = "event-1")))))

        // Act
        val thrown = runCatching {
            with(fetcher) {
                context.fetchUpdateAsync(toUpdate = context.batches, updateRequest = updateRequest(batchKey = 0))
            }
        }.exceptionOrNull()

        // Assert
        assertThat(thrown).isInstanceOf(IOException::class.java)
    }

    @Test
    fun `GIVEN a refreshed page WHEN it is merged THEN it keeps the cursor it was loaded with`() = runTest {
        // Act
        val context = refresh(
            page = batch(key = 0, events = listOf(event(id = "event-1")), cursor = "cursor-0"),
            response = listOf(event(id = "event-1")),
        )

        // Assert
        assertThat(context.batchOf(key = 0).requestCursor).isEqualTo("cursor-0")
    }

    private suspend fun refresh(
        page: Batch<Int, PolymarketEventsBatch>,
        response: List<PolymarketEvent>,
    ): FakeUpdateContext {
        val fetcher = PolymarketEventsUpdateFetcher(
            batchSize = BATCH_SIZE,
            fetchPage = { _, cursor, limit ->
                requestedCursors += cursor
                requestedLimits += limit
                PolymarketEventsPage(events = response, cursor = "next-cursor", hasNext = true)
            },
        )
        val context = FakeUpdateContext(batches = listOf(page))

        with(fetcher) {
            context.fetchUpdateAsync(toUpdate = context.batches, updateRequest = updateRequest(batchKey = page.key))
        }

        return context
    }

    private fun updateRequest(batchKey: Int) =
        PolymarketEventsUpdateRequest(batchKey = batchKey, config = config)

    private fun batch(key: Int, events: List<PolymarketEvent>, cursor: String? = null) = Batch(
        key = key,
        data = PolymarketEventsBatch(events = events, requestCursor = cursor),
    )

    private fun event(id: String): PolymarketEvent = mockk {
        every { this@mockk.id } returns id
    }

    /** Applies updates the way [com.tangem.pagination.BatchListSource] does: by key, over the current batches. */
    private class FakeUpdateContext(
        batches: List<Batch<Int, PolymarketEventsBatch>>,
    ) : BatchUpdateFetcher.UpdateContext<Int, PolymarketEventsBatch> {

        var batches: List<Batch<Int, PolymarketEventsBatch>> = batches
            private set

        override suspend fun update(
            update: List<Batch<Int, PolymarketEventsBatch>>.() -> BatchUpdateResult<Int, PolymarketEventsBatch>,
        ) {
            val result = batches.update()
            if (result !is BatchUpdateResult.Success) return

            val updated = result.data.associateBy { it.key }
            batches = batches.map { updated[it.key] ?: it }
        }

        fun batchOf(key: Int): PolymarketEventsBatch = batches.first { it.key == key }.data

        fun eventsOf(key: Int): List<PolymarketEvent> = batchOf(key).events
    }

    private companion object {
        const val BATCH_SIZE = 20
    }
}