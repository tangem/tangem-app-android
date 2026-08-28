package com.tangem.features.collectibles.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class CollectiblesMainModelTest {

    private val router: Router = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tabs {

        @Test
        fun `GIVEN model created WHEN category clicked THEN only its packs are left`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.tabs.selectedContent(id = "sports").onClick()

            // Assert
            val content = model.uiState.value.content as CollectiblesMainUM.ContentUM.Content
            assertThat(content.packs.map { it.id }).containsExactly("elite_1", "legendary_2")
            model.onDestroy()
        }

        @Test
        fun `GIVEN category selected WHEN state read THEN that tab is the selected one`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.tabs.selectedContent(id = "anime").onClick()

            // Assert
            val selected = model.uiState.value.tabs.filterIsInstance<TangemTabItemUM.Content>().single { it.isSelected }
            assertThat(selected.id).isEqualTo("anime")
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Search {

        @Test
        fun `GIVEN search active WHEN query typed THEN packs are filtered by title`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.search.onActiveChange(true)

            // Act
            model.uiState.value.search.onQueryChange("water")

            // Assert
            val content = model.uiState.value.content as CollectiblesMainUM.ContentUM.Content
            assertThat(content.packs.map { it.id }).containsExactly("water_1", "water_2")
            model.onDestroy()
        }

        @Test
        fun `GIVEN search active WHEN query matches nothing THEN content is empty`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.search.onActiveChange(true)

            // Act
            model.uiState.value.search.onQueryChange("Serhii")

            // Assert
            assertThat(model.uiState.value.content).isInstanceOf(CollectiblesMainUM.ContentUM.Empty::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN query typed WHEN search closed THEN query is reset and packs are unfiltered`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.search.onActiveChange(true)
            model.uiState.value.search.onQueryChange("water")

            // Act
            model.uiState.value.search.onCloseClick?.invoke()

            // Assert
            val state = model.uiState.value
            assertThat(state.search.query).isEmpty()
            assertThat(state.search.isActive).isFalse()
            val content = state.content as CollectiblesMainUM.ContentUM.Content
            assertThat(content.packs).hasSize(CollectiblesMainStubData.PACKS.size)
            model.onDestroy()
        }

        @Test
        fun `GIVEN query typed WHEN cleared THEN search stays active`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.search.onActiveChange(true)
            model.uiState.value.search.onQueryChange("water")

            // Act
            model.uiState.value.search.onClearClick()

            // Assert
            val state = model.uiState.value
            assertThat(state.search.query).isEmpty()
            assertThat(state.search.isActive).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN search active WHEN state read THEN tabs are kept for the screen to hide`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            val tabs = model.uiState.value.tabs

            // Act
            model.uiState.value.search.onActiveChange(true)

            // Assert
            assertThat(model.uiState.value.tabs).isEqualTo(tabs)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Menu {

        @Test
        fun `GIVEN model created WHEN menu clicked THEN menu is shown`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.menu.onMenuClick()

            // Assert
            assertThat(model.uiState.value.menu.isShown).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN menu shown WHEN dismissed THEN menu is hidden`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.onDismiss()

            // Assert
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN menu shown WHEN how it works clicked THEN stories open`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.items.single { it.id == "how_it_works" }.onClick()

            // Assert
            verify(exactly = 1) { router.push(CollectiblesRoute.Stories, onComplete = any()) }
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }
    }

    private fun List<TangemTabItemUM>.selectedContent(id: String): TangemTabItemUM.Content =
        filterIsInstance<TangemTabItemUM.Content>().single { it.id == id }

    private fun createModel(testScope: TestScope): CollectiblesMainModel {
        return CollectiblesMainModel(
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}