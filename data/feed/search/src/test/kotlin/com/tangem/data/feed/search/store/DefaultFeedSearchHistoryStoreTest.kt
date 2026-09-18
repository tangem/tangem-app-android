package com.tangem.data.feed.search.store

import com.google.common.truth.Truth.assertThat
import com.tangem.data.feed.search.model.FeedSearchHistoryDTO
import com.tangem.data.feed.search.model.QueryDTO
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository.Companion.MAX_HISTORY_SIZE
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultFeedSearchHistoryStoreTest {

    private val dataStore = MockStateDataStore(default = FeedSearchHistoryDTO())

    private val store = DefaultFeedSearchHistoryStore(dataStore = dataStore)

    @Test
    fun `GIVEN query already stored WHEN saveQuery THEN moved to top without duplicate`() = runTest {
        // Arrange
        store.saveQuery(QueryDTO(text = "bitcoin", timestamp = 1))
        store.saveQuery(QueryDTO(text = "apple", timestamp = 2))

        // Act
        store.saveQuery(QueryDTO(text = "bitcoin", timestamp = 3))

        // Assert
        assertThat(store.getQueries().first().map(QueryDTO::text)).containsExactly("bitcoin", "apple").inOrder()
    }

    @Test
    fun `GIVEN history is full WHEN saveQuery THEN oldest entry is evicted`() = runTest {
        // Arrange
        repeat(MAX_HISTORY_SIZE) { index ->
            store.saveQuery(QueryDTO(text = "query-$index", timestamp = index.toLong()))
        }

        // Act
        store.saveQuery(QueryDTO(text = "newcomer", timestamp = MAX_HISTORY_SIZE.toLong()))

        // Assert
        val texts = store.getQueries().first().map(QueryDTO::text)
        assertThat(texts).hasSize(MAX_HISTORY_SIZE)
        assertThat(texts.first()).isEqualTo("newcomer")
        assertThat(texts).doesNotContain("query-0")
    }

    @Test
    fun `GIVEN queries stored out of order WHEN getQueries THEN newest comes first`() = runTest {
        // Arrange
        store.saveQuery(QueryDTO(text = "older", timestamp = 100))
        store.saveQuery(QueryDTO(text = "newest", timestamp = 300))
        store.saveQuery(QueryDTO(text = "middle", timestamp = 200))

        // Act
        val texts = store.getQueries().first().map(QueryDTO::text)

        // Assert
        assertThat(texts).containsExactly("newest", "middle", "older").inOrder()
    }

    @Test
    fun `GIVEN queries stored WHEN removeQuery THEN only that query is dropped`() = runTest {
        // Arrange
        store.saveQuery(QueryDTO(text = "bitcoin", timestamp = 1))
        store.saveQuery(QueryDTO(text = "apple", timestamp = 2))

        // Act
        store.removeQuery(text = "apple")

        // Assert
        assertThat(store.getQueries().first().map(QueryDTO::text)).containsExactly("bitcoin")
    }
}