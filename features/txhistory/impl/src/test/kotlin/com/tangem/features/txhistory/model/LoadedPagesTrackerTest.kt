package com.tangem.features.txhistory.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.TxHistoryInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class LoadedPagesTrackerTest {

    private val tracker = LoadedPagesTracker()

    @Test
    fun `GIVEN a list with a next page WHEN more is requested THEN more pages count as loaded`() {
        // Act
        tracker.onLoadMore(current = content(size = 2, hasMore = true))

        // Assert
        assertThat(tracker.hasLoadedMorePages.value).isTrue()
    }

    @Test
    fun `GIVEN a list without a next page WHEN more is requested THEN nothing counts as loaded`() {
        // Act
        tracker.onLoadMore(current = content(size = 2, hasMore = false))

        // Assert
        assertThat(tracker.hasLoadedMorePages.value).isFalse()
    }

    @Test
    fun `GIVEN the legacy list WHEN more is requested THEN more pages count as loaded`() {
        // Act
        tracker.onLoadMore(current = null)

        // Assert
        assertThat(tracker.hasLoadedMorePages.value).isTrue()
    }

    @Test
    fun `GIVEN a requested page WHEN it lands with more rows THEN more pages stay loaded`() {
        // Arrange
        tracker.onLoadMore(current = content(size = 2, hasMore = true))
        tracker.onState(content(size = 2, hasMore = true, isLoadingMore = true))

        // Act
        tracker.onState(content(size = 4, hasMore = true))

        // Assert
        assertThat(tracker.hasLoadedMorePages.value).isTrue()
    }

    @Test
    fun `GIVEN a requested page WHEN loading ends without new rows THEN nothing counts as loaded`() {
        // Arrange
        tracker.onLoadMore(current = content(size = 2, hasMore = true))
        tracker.onState(content(size = 2, hasMore = true, isLoadingMore = true))

        // Act
        tracker.onState(content(size = 2, hasMore = true))

        // Assert
        assertThat(tracker.hasLoadedMorePages.value).isFalse()
    }

    @Test
    fun `GIVEN loaded pages WHEN the list is reloaded THEN nothing counts as loaded`() {
        // Arrange
        tracker.onLoadMore(current = content(size = 2, hasMore = true))
        tracker.onState(content(size = 4, hasMore = true))

        // Act
        tracker.onReload()

        // Assert
        assertThat(tracker.hasLoadedMorePages.value).isFalse()
    }

    private fun content(size: Int, hasMore: Boolean, isLoadingMore: Boolean = false) = HistoryState.Content(
        items = List(size) { index -> mockk<TxHistoryInfo> { every { txId } returns "tx-$index" } },
        isLoadingMore = isLoadingMore,
        hasMore = hasMore,
    )
}