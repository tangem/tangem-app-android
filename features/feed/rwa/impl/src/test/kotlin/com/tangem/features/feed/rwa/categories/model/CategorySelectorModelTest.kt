package com.tangem.features.feed.rwa.categories.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent.ModelCallback
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.features.feed.rwa.model.UserCountryResolver
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Every pick applies and closes; chips show under every category with a sector choice, and a picked
 * sector takes the check from its category row.
 * The umbrella id never travels in a selection, and Featured is not a filter at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CategorySelectorModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase = mockk()
    private val userCountryResolver: UserCountryResolver = mockk()
    private val feedFeatureToggles: FeedFeatureToggles = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, getCoinCategoriesUseCase, userCountryResolver, feedFeatureToggles)
        coEvery { userCountryResolver.resolve() } returns "DE"
        every { feedFeatureToggles.isCategoryFiltersRedesignEnabled } returns true
    }

    @Test
    fun `GIVEN featured and hidden categories WHEN model created THEN only filterable ones are offered`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks, featured, hidden)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().items.map { it.id }).containsExactly(umbrella.id, stocks.id).inOrder()
    }

    @Test
    fun `GIVEN a filter applied WHEN umbrella row tapped THEN the filter is cleared and the screen closes`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapCategory(umbrella.id)

            // Assert
            assertThat(callback.selections).containsExactly(CategorySelection())
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN a restricted region WHEN umbrella row tapped THEN the filter is cleared rather than defaulted`() =
        runTest {
            // Arrange — "All" is the reset, and it means no filter even where the tab opens on commodities
            coEvery { userCountryResolver.resolve() } returns "US"
            stubCategories(umbrella, stocks, commodities, country = "US")
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = commoditiesSelection, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapCategory(umbrella.id)

            // Assert
            assertThat(callback.selections).containsExactly(CategorySelection())
        }

    @Test
    fun `GIVEN umbrella applied WHEN model created THEN its row is the checked one`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val checked = model.content().items.filter { it.isSelected }
        assertThat(checked.map { it.id }).containsExactly(umbrella.id)
    }

    @Test
    fun `GIVEN category with sector applied WHEN a category without sector choice tapped THEN it applies alone`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks, bonds)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapCategory(bonds.id)

            // Assert
            assertThat(callback.selections).containsExactly(bondsSelection)
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN a category without sector choice applied WHEN tapped again THEN the tab is not told`() =
        runTest {
            // Arrange
            stubCategories(umbrella, bonds)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = bondsSelection, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapCategory(bonds.id)

            // Assert
            assertThat(callback.selections).isEmpty()
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN no filter WHEN a category with sectors tapped THEN it applies and the screen closes`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, callback = callback)
        advanceUntilIdle()

        // Act
        model.tapCategory(stocks.id)

        // Assert
        assertThat(callback.selections).containsExactly(stocksSelection)
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN a category with sectors applied WHEN a chip tapped THEN the pair applies and the screen closes`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = stocksSelection, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapSector(categoryId = stocks.id, sectorId = "technology")

            // Assert
            assertThat(callback.selections).containsExactly(stocksWithTechnology)
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN another category applied WHEN a chip tapped THEN its own category applies with the sector`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks, bonds)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = bondsSelection, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapSector(categoryId = stocks.id, sectorId = "technology")

            // Assert
            assertThat(callback.selections).containsExactly(stocksWithTechnology)
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN a sector applied WHEN its chip tapped again THEN the whole category applies`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()

        // Act
        model.tapSector(categoryId = stocks.id, sectorId = "technology")

        // Assert
        assertThat(callback.selections).containsExactly(stocksSelection)
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN a sector applied WHEN model created THEN its chip is checked and its category row is not`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)

        // Act
        val model = createModel(testScope = this, selection = stocksWithTechnology)
        advanceUntilIdle()

        // Assert
        assertThat(model.checkedCategoryIds()).isEmpty()
        assertThat(model.sectors(stocks.id).single { it.isSelected }.id).isEqualTo("technology")
    }

    @Test
    fun `GIVEN category with sector applied WHEN same category tapped THEN the sector is dropped`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()

        // Act
        model.tapCategory(stocks.id)

        // Assert
        assertThat(callback.selections).containsExactly(stocksSelection)
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN a live pick WHEN the tab echoes it back THEN it is not applied twice`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, callback = callback)
        advanceUntilIdle()

        // Act
        model.tapCategory(stocks.id)
        model.tapSector(categoryId = stocks.id, sectorId = "healthcare")
        advanceUntilIdle()

        // Assert
        assertThat(callback.selections).containsExactly(stocksSelection, stocksWithHealthcare).inOrder()
    }

    @Test
    fun `GIVEN a category with a lone sector WHEN model created THEN its row shows no chips`() = runTest {
        // Arrange
        stubCategories(umbrella, bonds)

        // Act
        val model = createModel(testScope = this, selection = bondsSelection)
        advanceUntilIdle()

        // Assert
        assertThat(model.sectors(bonds.id)).isEmpty()
    }

    @Test
    fun `GIVEN another category applied WHEN model created THEN every category with sectors shows its chips`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks, bonds)

            // Act
            val model = createModel(testScope = this, selection = bondsSelection)
            advanceUntilIdle()

            // Assert
            assertThat(model.content().items.filter { it.sectors.isNotEmpty() }.map { it.id })
                .containsExactly(stocks.id)
            assertThat(model.sectors(stocks.id).map { it.id }).containsExactly("technology", "healthcare").inOrder()
            assertThat(model.sectors(stocks.id).none { it.isSelected }).isTrue()
        }

    @Test
    fun `GIVEN failing categories request WHEN model created THEN error state offers a retry`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Left(IllegalStateException("no categories"))

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
        assertThat(model.content().items.map { it.id }).containsExactly(umbrella.id, stocks.id)
    }

    @Test
    fun `GIVEN a sector whose id differs from its code WHEN model created THEN the label comes from the code`() =
        runTest {
            // Arrange — a backend serving uuid ids must still resolve names through the stable code
            val xstocks = createCategory(
                id = "6",
                code = "xstocks",
                displayName = "xStocks",
                displayOrder = 6,
                sectors = listOf(
                    createSector(id = "3f9a-uuid", code = "technology", displayName = "Technology"),
                    createSector(id = "7b2c-uuid", code = "healthcare", displayName = "Healthcare"),
                ),
            )
            stubCategories(umbrella, xstocks)
            val selection = CategorySelection(
                category = CategorySelection.Entry(
                    id = xstocks.id,
                    displayName = xstocks.displayName,
                    code = xstocks.code,
                ),
            )

            // Act
            val model = createModel(testScope = this, selection = selection)
            advanceUntilIdle()

            // Assert
            assertThat(model.sectors(xstocks.id).map { it.label }).containsExactly(
                resourceReference(R.string.markets_real_assets_sector_xstocks_technology),
                resourceReference(R.string.markets_real_assets_sector_xstocks_healthcare),
            ).inOrder()
        }

    @Test
    fun `GIVEN an open selector WHEN the tab applies another sector THEN it is ticked without applying`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks, bonds)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = bondsSelection, callback = callback)
            advanceUntilIdle()

            // Act
            callback.appliedSelection.value = stocksWithTechnology
            advanceUntilIdle()

            // Assert - the deeplink already filtered the list, so nothing is applied back
            assertThat(model.checkedCategoryIds()).isEmpty()
            assertThat(model.sectors(stocks.id).single { it.isSelected }.id).isEqualTo("technology")
            assertThat(callback.selections).isEmpty()
            verify(exactly = 0) { router.pop(any()) }
        }

    private fun createModel(
        testScope: TestScope,
        selection: CategorySelection = CategorySelection(),
        callback: RecordingCallback = RecordingCallback(),
    ): CategorySelectorModel {
        // the tab pushes the selector with the selection its list is filtered by
        callback.appliedSelection.value = selection

        return CategorySelectorModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                CategorySelectorRoute(categorySelection = selection, callback = callback),
            ),
            router = router,
            getCoinCategoriesUseCase = getCoinCategoriesUseCase,
            userCountryResolver = userCountryResolver,
            feedFeatureToggles = feedFeatureToggles,
        )
    }

    private fun stubCategories(vararg categories: CoinCategory, country: String = "DE") {
        coEvery { getCoinCategoriesUseCase(country = country) } returns Either.Right(categories.toList())
    }

    private fun CategorySelectorModel.content() = uiState.value as CategorySelectorUM.Content

    private fun CategorySelectorModel.tapCategory(id: String) = content().items.single { it.id == id }.onItemClick()

    private fun CategorySelectorModel.tapSector(categoryId: String, sectorId: String) =
        sectors(categoryId).single { it.id == sectorId }.onItemClick()

    private fun CategorySelectorModel.checkedCategoryIds() = content().items.filter { it.isSelected }.map { it.id }

    private fun CategorySelectorModel.sectors(categoryId: String) = content().items.single { it.id == categoryId }.sectors

    /** Echoes every pick back through [appliedSelection], as the tab does. */
    private class RecordingCallback : ModelCallback {

        override val appliedSelection = MutableStateFlow(CategorySelection())

        val selections = mutableListOf<CategorySelection>()

        override fun onCategorySelect(selection: CategorySelection) {
            selections += selection
            appliedSelection.value = selection
        }
    }

    private companion object {

        val umbrella = createCategory(
            id = "1",
            code = "real-world-assets",
            displayName = "Real world assets",
            isVisible = false,
        )

        val stocks = createCategory(
            id = "2",
            code = "stocks",
            displayName = "xStocks",
            displayOrder = 2,
            sectors = listOf(
                createSector(id = "technology", displayName = "Technology", displayOrder = 0, tokensCount = 48),
                createSector(id = "healthcare", displayName = "Healthcare", displayOrder = 1, tokensCount = 31),
            ),
        )

        val bonds = createCategory(
            id = "3",
            code = "bonds",
            displayName = "Bonds",
            displayOrder = 3,
            sectors = listOf(createSector(id = "government", displayName = "Government", tokensCount = 7)),
        )

        val featured = createCategory(
            id = "4",
            code = ReservedCategoryCodes.FEATURED_ASSETS,
            displayName = "Featured",
            displayOrder = 4,
        )

        val hidden = createCategory(
            id = "5",
            code = "gems",
            displayName = "Gems",
            displayOrder = 5,
            isVisible = false,
        )

        val commodities = createCategory(
            id = "6",
            code = ReservedCategoryCodes.COMMODITIES,
            displayName = "Commodities",
            displayOrder = 6,
        )

        val commoditiesSelection = CategorySelection(
            category = CategorySelection.Entry(
                id = commodities.id,
                displayName = commodities.displayName,
                code = commodities.code,
            ),
        )

        val bondsSelection = CategorySelection(
            category = CategorySelection.Entry(id = bonds.id, displayName = bonds.displayName, code = bonds.code),
        )

        val stocksSelection = CategorySelection(
            category = CategorySelection.Entry(id = stocks.id, displayName = stocks.displayName, code = stocks.code),
        )

        val stocksWithHealthcare = stocksSelection.copy(
            sector = CategorySelection.Entry(id = "healthcare", displayName = "Healthcare", code = "healthcare"),
        )

        val stocksWithTechnology = CategorySelection(
            category = CategorySelection.Entry(id = stocks.id, displayName = stocks.displayName, code = stocks.code),
            sector = CategorySelection.Entry(id = "technology", displayName = "Technology", code = "technology"),
        )

        fun createSector(
            id: String,
            displayName: String,
            code: String = id,
            displayOrder: Int = 0,
            tokensCount: Int = 7,
        ) = CoinCategory.Sector(
            id = id,
            code = code,
            displayName = displayName,
            displayOrder = displayOrder,
            tokensCount = tokensCount,
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