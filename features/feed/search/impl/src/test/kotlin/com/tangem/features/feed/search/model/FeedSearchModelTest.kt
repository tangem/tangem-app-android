package com.tangem.features.feed.search.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.feed.search.usecase.*
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.FeedSearchBarUM
import com.tangem.features.feed.search.model.analytics.FeedSearchAnalyticsEvent
import com.tangem.features.feed.search.ui.state.RecentQueryUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedSearchModelTest {

    private val getRecentFeedSearchQueriesUseCase: GetRecentFeedSearchQueriesUseCase = mockk()
    private val removeFeedSearchQueryUseCase: RemoveFeedSearchQueryUseCase = mockk(relaxed = true)
    private val saveFeedSearchQueryUseCase: SaveFeedSearchQueryUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getRecentFeedSearchQueriesUseCase,
            removeFeedSearchQueryUseCase,
            saveFeedSearchQueryUseCase,
            analyticsEventHandler,
        )
    }

    @Test
    fun `GIVEN recent queries WHEN query typed THEN state exposes it with the 3 newest queries`() = runTest {
        // Arrange
        stubHistory(queries = listOf("D. Trump", "Apple", "Dollar", "Gram", "Ether"))

        // Act
        val model = createModel(testScope = this, searchBarController = FakeSearchBarController("bitc"))
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.query).isEqualTo("bitc")
        assertThat(state.recentQueries.map(RecentQueryUM::text))
            .containsExactly("D. Trump", "Apple", "Dollar")
            .inOrder()

        model.onDestroy()
    }

    @Test
    fun `GIVEN empty history WHEN model created THEN there are no recent queries`() = runTest {
        // Arrange
        stubHistory(queries = emptyList())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.recentQueries).isEmpty()

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent query WHEN it is clicked THEN it is put back into the search bar`() = runTest {
        // Arrange
        stubHistory(queries = listOf("Apple"))
        val searchBarController = FakeSearchBarController(initialQuery = "")
        val model = createModel(testScope = this, searchBarController = searchBarController)
        advanceUntilIdle()

        // Act
        model.uiState.value.recentQueries.first().onClick()
        advanceUntilIdle()

        // Assert
        assertThat(searchBarController.state.value.query).isEqualTo("Apple")
        assertThat(model.uiState.value.query).isEqualTo("Apple")

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent query WHEN its remove is clicked THEN only that query is removed`() = runTest {
        // Arrange
        stubHistory(queries = listOf("Apple", "Dollar"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recentQueries.first().onRemoveClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { removeFeedSearchQueryUseCase("Apple") }
        coVerify(exactly = 0) { removeFeedSearchQueryUseCase("Dollar") }

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent query WHEN it is clicked THEN a hint-clicked event is sent`() = runTest {
        // Arrange
        stubHistory(queries = listOf("Apple"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recentQueries.first().onClick()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(FeedSearchAnalyticsEvent.HintClicked("Apple")) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a typed query WHEN the keyboard search action fires THEN the query is saved`() = runTest {
        // Arrange
        stubHistory(queries = emptyList())
        val searchBarController = FakeSearchBarController(initialQuery = "bitcoin")
        val model = createModel(testScope = this, searchBarController = searchBarController)
        advanceUntilIdle()

        // Act
        searchBarController.onSubmit()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { saveFeedSearchQueryUseCase("bitcoin") }

        model.onDestroy()
    }

    @Test
    fun `GIVEN no submit WHEN the query is only typed THEN nothing is saved`() = runTest {
        // Arrange
        stubHistory(queries = emptyList())
        val searchBarController = FakeSearchBarController(initialQuery = "")
        val model = createModel(testScope = this, searchBarController = searchBarController)

        // Act
        searchBarController.onQueryChange("bitc")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { saveFeedSearchQueryUseCase(any()) }

        model.onDestroy()
    }

    private fun stubHistory(queries: List<String>) {
        every { getRecentFeedSearchQueriesUseCase() } returns flowOf(queries)
    }

    private fun createModel(
        testScope: TestScope,
        searchBarController: FeedSearchBarController = FakeSearchBarController(initialQuery = ""),
    ): FeedSearchModel {
        return FeedSearchModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(FeedRoute.Search(source = SOURCE)),
            searchBarController = searchBarController,
            analyticsEventHandler = analyticsEventHandler,
            removeFeedSearchQueryUseCase = removeFeedSearchQueryUseCase,
            saveFeedSearchQueryUseCase = saveFeedSearchQueryUseCase,
            getRecentFeedSearchQueriesUseCase = getRecentFeedSearchQueriesUseCase,
        )
    }

    private class FakeSearchBarController(initialQuery: String) : FeedSearchBarController {

        override val state: MutableStateFlow<FeedSearchBarUM> =
            MutableStateFlow(FeedSearchBarUM(query = initialQuery, isActive = true))

        override val submits: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)

        override fun onQueryChange(query: String) {
            state.update { it.copy(query = query) }
        }

        override fun onActiveChange(isActive: Boolean) {
            state.update { it.copy(isActive = isActive) }
        }

        override fun onSubmit() {
            submits.tryEmit(Unit)
        }
    }

    private companion object {

        const val SOURCE = "markets"
    }
}