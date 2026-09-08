package com.tangem.features.feed.rwa.categories.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent.ModelCallback
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorSectionUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** The umbrella id must never travel in a selection, and Featured is not a filter at all. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CategorySelectorModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, getCoinCategoriesUseCase)
    }

    @Test
    fun `GIVEN featured and hidden categories WHEN model created THEN only filterable ones are offered`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks, featured, hidden)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.items().items.map { it.id }).containsExactly(umbrella.id, stocks.id).inOrder()
    }

    @Test
    fun `GIVEN no filter WHEN umbrella row tapped THEN the applied selection stays empty`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, callback = callback)
        advanceUntilIdle()

        // Act
        model.items().items.first { it.id == umbrella.id }.onItemClick()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(CategorySelection())
    }

    @Test
    fun `GIVEN umbrella applied WHEN model created THEN its row is the checked one`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val checked = model.items().items.filter { it.isSelected }
        assertThat(checked.map { it.id }).containsExactly(umbrella.id)
    }

    @Test
    fun `GIVEN category with sector applied WHEN the same category tapped THEN the sector survives`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()

        // Act
        model.items().items.first { it.id == stocks.id }.onItemClick()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(stocksWithTechnology)
    }

    @Test
    fun `GIVEN category with sector applied WHEN another category tapped THEN the sector is dropped`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks, bonds)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()

        // Act
        model.items().items.first { it.id == bonds.id }.onItemClick()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(
            CategorySelection(category = CategorySelection.Entry(id = bonds.id, displayName = bonds.displayName)),
        )
    }

    @Test
    fun `GIVEN selected category with a lone sector WHEN model created THEN the sectors section is hidden`() =
        runTest {
            // Arrange
            stubCategories(umbrella, bonds)
            val selection = CategorySelection(
                category = CategorySelection.Entry(id = bonds.id, displayName = bonds.displayName),
            )

            // Act
            val model = createModel(testScope = this, selection = selection)
            advanceUntilIdle()

            // Assert
            assertThat(model.sectors()).isNull()
        }

    @Test
    fun `GIVEN selected category with two sectors WHEN model created THEN the section leads with All`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val selection = CategorySelection(
            category = CategorySelection.Entry(id = stocks.id, displayName = stocks.displayName),
        )

        // Act
        val model = createModel(testScope = this, selection = selection)
        advanceUntilIdle()

        // Assert
        val sectors = requireNotNull(model.sectors())
        assertThat(sectors.sectors.map { it.id }).containsExactly("__all__", "technology", "healthcare").inOrder()
        assertThat(sectors.sectors.first().isSelected).isTrue()
    }

    @Test
    fun `GIVEN edited selection WHEN reset tapped THEN the applied one comes back`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()
        model.items().items.first { it.id == umbrella.id }.onItemClick()

        // Act
        model.content().footer.onReset()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(stocksWithTechnology)
    }

    @Test
    fun `GIVEN edited selection WHEN apply tapped THEN the screen is popped`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.content().footer.onApply()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN failing categories request WHEN model created THEN error state offers a retry`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase() } returns Either.Left(IllegalStateException("no categories"))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(CategorySelectorUM.Error::class.java)

        // Arrange
        stubCategories(umbrella, stocks)

        // Act
        (state as CategorySelectorUM.Error).onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(model.items().items.map { it.id }).containsExactly(umbrella.id, stocks.id)
    }

    private fun stubCategories(vararg categories: CoinCategory) {
        coEvery { getCoinCategoriesUseCase() } returns Either.Right(categories.toList())
    }

    private fun createModel(
        testScope: TestScope,
        selection: CategorySelection = CategorySelection(),
        callback: ModelCallback = RecordingCallback(),
    ): CategorySelectorModel {
        return CategorySelectorModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                CategorySelectorRoute(categorySelection = selection, callback = callback),
            ),
            router = router,
            getCoinCategoriesUseCase = getCoinCategoriesUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
        )
    }

    private fun CategorySelectorModel.content() = uiState.value as CategorySelectorUM.Content

    private fun CategorySelectorModel.items() = content().sections
        .filterIsInstance<CategorySelectorSectionUM.Items>()
        .single()

    private fun CategorySelectorModel.sectors() = content().sections
        .filterIsInstance<CategorySelectorSectionUM.Sectors>()
        .singleOrNull()

    private class RecordingCallback : ModelCallback {

        var selection: CategorySelection? = null
            private set

        override fun onCategorySelect(selection: CategorySelection) {
            this.selection = selection
        }
    }

    private companion object {

        val umbrella = createCategory(id = "1", code = "real-world-assets", displayName = "Real world assets")

        val stocks = createCategory(
            id = "2",
            code = "stocks",
            displayName = "xStocks",
            displayOrder = 2,
            sectors = listOf(
                CoinCategory.Sector(id = "technology", displayName = "Technology", tokensCount = 48),
                CoinCategory.Sector(id = "healthcare", displayName = "Healthcare", tokensCount = 31),
            ),
        )

        val bonds = createCategory(
            id = "3",
            code = "bonds",
            displayName = "Bonds",
            displayOrder = 3,
            sectors = listOf(CoinCategory.Sector(id = "government", displayName = "Government", tokensCount = 7)),
        )

        val featured = createCategory(id = "4", code = "featured_assets", displayName = "Featured", displayOrder = 4)

        val hidden = createCategory(
            id = "5",
            code = "commodities",
            displayName = "Commodities",
            displayOrder = 5,
            isVisible = false,
        )

        val stocksWithTechnology = CategorySelection(
            category = CategorySelection.Entry(id = stocks.id, displayName = stocks.displayName),
            sector = CategorySelection.Entry(id = "technology", displayName = "Technology"),
        )

        @Suppress("LongParameterList")
        fun createCategory(
            id: String,
            code: String,
            displayName: String,
            displayOrder: Int = 1,
            tokensCount: Int = 212,
            isRestricted: Boolean = false,
            isVisible: Boolean = true,
            sectors: List<CoinCategory.Sector> = emptyList(),
        ) = CoinCategory(
            id = id,
            code = code,
            displayName = displayName,
            displayOrder = displayOrder,
            tokensCount = tokensCount,
            isRestricted = isRestricted,
            sectors = sectors,
            isVisible = isVisible,
        )
    }
}