package com.tangem.data.feed.search.store

import com.google.common.truth.Truth.assertThat
import com.tangem.data.feed.search.model.FeedSearchHistoryDTO
import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.data.feed.search.model.ItemKind
import com.tangem.data.feed.search.model.MarketTokenDTO
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
    fun `GIVEN item already stored WHEN saveItem THEN moved to top without duplicate`() = runTest {
        // Arrange
        store.saveItem(marketToken(id = "bitcoin", timestamp = 1))
        store.saveItem(marketToken(id = "tether", timestamp = 2))

        // Act
        store.saveItem(marketToken(id = "bitcoin", timestamp = 3))

        // Assert
        assertThat(store.getItems().first().map(ItemDTO::id)).containsExactly("bitcoin", "tether").inOrder()
    }

    @Test
    fun `GIVEN history is full WHEN saveItem THEN oldest entry is evicted`() = runTest {
        // Arrange
        repeat(MAX_HISTORY_SIZE) { index ->
            store.saveItem(marketToken(id = "token-$index", timestamp = index.toLong()))
        }

        // Act
        store.saveItem(marketToken(id = "newcomer", timestamp = MAX_HISTORY_SIZE.toLong()))

        // Assert
        val ids = store.getItems().first().map(ItemDTO::id)
        assertThat(ids).hasSize(MAX_HISTORY_SIZE)
        assertThat(ids.first()).isEqualTo("newcomer")
        assertThat(ids).doesNotContain("token-0")
    }

    @Test
    fun `GIVEN items stored out of order WHEN getItems THEN newest comes first`() = runTest {
        // Arrange
        store.saveItem(marketToken(id = "older", timestamp = 100))
        store.saveItem(marketToken(id = "newest", timestamp = 300))
        store.saveItem(marketToken(id = "middle", timestamp = 200))

        // Act
        val ids = store.getItems().first().map(ItemDTO::id)

        // Assert
        assertThat(ids).containsExactly("newest", "middle", "older").inOrder()
    }

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
    fun `GIVEN queries and items stored WHEN removeQuery THEN only that query is dropped`() = runTest {
        // Arrange
        store.saveQuery(QueryDTO(text = "bitcoin", timestamp = 1))
        store.saveQuery(QueryDTO(text = "apple", timestamp = 2))
        store.saveItem(marketToken(id = "bitcoin", timestamp = 3))

        // Act
        store.removeQuery(text = "apple")

        // Assert
        assertThat(store.getQueries().first().map(QueryDTO::text)).containsExactly("bitcoin")
        assertThat(store.getItems().first().map(ItemDTO::id)).containsExactly("bitcoin")
    }

    @Test
    fun `GIVEN queries and items stored WHEN clear THEN both lists are empty`() = runTest {
        // Arrange
        store.saveQuery(QueryDTO(text = "bitcoin", timestamp = 1))
        store.saveItem(marketToken(id = "bitcoin", timestamp = 2))

        // Act
        store.clear()

        // Assert
        assertThat(store.getQueries().first()).isEmpty()
        assertThat(store.getItems().first()).isEmpty()
    }

    private fun marketToken(id: String, timestamp: Long) = ItemDTO(
        kind = ItemKind.MARKET_TOKEN,
        id = id,
        timestamp = timestamp,
        marketToken = MarketTokenDTO(
            name = id,
            symbol = id.take(n = 3).uppercase(),
            currentPrice = "1",
        ),
    )
}