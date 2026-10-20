package com.tangem.features.feed.search.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.FeedSearchBarUM
import com.tangem.features.feed.search.model.analytics.FeedSearchAnalyticsEvent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedSearchModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @Test
    fun `GIVEN a typed query WHEN it changes THEN the model follows the search bar`() = runTest {
        // Arrange
        val searchBarController = FakeSearchBarController(initialQuery = "b")
        val model = createModel(testScope = this, searchBarController = searchBarController)
        advanceUntilIdle()

        // Act
        searchBarController.onQueryChange("bitc")
        advanceUntilIdle()

        // Assert
        assertThat(model.query.value).isEqualTo("bitc")

        model.onDestroy()
    }

    @Test
    fun `GIVEN a typed query WHEN the bar is cleared THEN the model keeps the last query`() = runTest {
        // Arrange
        val searchBarController = FakeSearchBarController(initialQuery = "bitc")
        val model = createModel(testScope = this, searchBarController = searchBarController)
        advanceUntilIdle()

        // Act
        searchBarController.onQueryChange("")
        advanceUntilIdle()

        // Assert
        assertThat(model.query.value).isEqualTo("bitc")

        model.onDestroy()
    }

    @Test
    fun `GIVEN screen opened WHEN model created THEN search screen opened is sent with its source`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(FeedSearchAnalyticsEvent.SearchScreenOpened(SOURCE)) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN screen opened by the first typed character WHEN more is typed THEN search started is sent once`() =
        runTest {
            // Arrange
            val searchBarController = FakeSearchBarController(initialQuery = "b")

            // Act
            val model = createModel(testScope = this, searchBarController = searchBarController)
            advanceUntilIdle()
            searchBarController.onQueryChange("bi")
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ofType<FeedSearchAnalyticsEvent.SearchStarted>()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN empty query WHEN model created THEN search started is not sent`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<FeedSearchAnalyticsEvent.SearchStarted>()) }

        model.onDestroy()
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
        )
    }

    private class FakeSearchBarController(initialQuery: String) : FeedSearchBarController {

        override val state: MutableStateFlow<FeedSearchBarUM> =
            MutableStateFlow(FeedSearchBarUM(query = initialQuery, isActive = true))

        override fun onQueryChange(query: String) {
            state.update { it.copy(query = query) }
        }

        override fun onFocusChange(isFocused: Boolean) = Unit

        override fun onClose() {
            state.value = FeedSearchBarUM()
        }
    }

    private companion object {

        const val SOURCE = "markets"
    }
}