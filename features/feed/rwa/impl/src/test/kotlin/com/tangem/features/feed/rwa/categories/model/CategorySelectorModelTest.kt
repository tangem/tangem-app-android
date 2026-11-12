package com.tangem.features.feed.rwa.categories.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent.ModelCallback
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorSectionUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.features.feed.rwa.model.UserCountryResolver
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A tap applies and closes, except on a category with a sector choice, which Apply confirms. The umbrella
 * id never travels in a selection, and Featured is not a filter at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CategorySelectorModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase = mockk()
    private val userCountryResolver: UserCountryResolver = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, getCoinCategoriesUseCase, userCountryResolver)
        coEvery { userCountryResolver.resolve() } returns "DE"
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
            assertThat(callback.selection).isEqualTo(CategorySelection())
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
            assertThat(callback.selection).isEqualTo(bondsSelection)
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
            assertThat(callback.selection).isNull()
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN no filter WHEN a category with sectors tapped THEN Apply shows and nothing applies`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapCategory(stocks.id)

            // Assert
            assertThat(model.sectors()).isNotNull()
            assertThat(model.content().isApplyVisible).isTrue()
            assertThat(callback.selection).isNull()
            verify(exactly = 0) { router.pop(any()) }
        }

    @Test
    fun `GIVEN a picked category with sectors WHEN a sector tapped and applied THEN the pair reaches the tab`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, callback = callback)
            advanceUntilIdle()
            model.tapCategory(stocks.id)

            // Act
            model.tapSector("technology")
            model.content().footer.onApply()

            // Assert
            assertThat(callback.selection).isEqualTo(stocksWithTechnology)
            verify(exactly = 1) { router.pop(any()) }
        }

    @Test
    fun `GIVEN category with sector applied WHEN its All sector applied THEN the whole category reaches the tab`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapSector("__all__")
            model.content().footer.onApply()

            // Assert
            assertThat(callback.selection).isEqualTo(CategorySelection(category = stocksWithTechnology.category))
        }

    @Test
    fun `GIVEN category with sector applied WHEN the same category tapped THEN the sector survives unapplied`() =
        runTest {
            // Arrange
            stubCategories(umbrella, stocks)
            val callback = RecordingCallback()
            val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
            advanceUntilIdle()

            // Act
            model.tapCategory(stocks.id)

            // Assert
            val selectedSectors = requireNotNull(model.sectors()).sectors.filter { it.isSelected }
            assertThat(selectedSectors.map { it.id }).containsExactly("technology")
            assertThat(model.content().isApplyVisible).isFalse()
            assertThat(callback.selection).isNull()
            verify(exactly = 0) { router.pop(any()) }
        }

    @Test
    fun `GIVEN another sector picked WHEN the applied one is picked back THEN Apply is hidden again`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val model = createModel(testScope = this, selection = stocksWithTechnology)
        advanceUntilIdle()
        model.tapSector("healthcare")

        // Act
        model.tapSector("technology")

        // Assert
        assertThat(model.content().isApplyVisible).isFalse()
    }

    @Test
    fun `GIVEN selected category with a lone sector WHEN model created THEN the sectors section is hidden`() =
        runTest {
            // Arrange
            stubCategories(umbrella, bonds)

            // Act
            val model = createModel(testScope = this, selection = bondsSelection)
            advanceUntilIdle()

            // Assert
            assertThat(model.sectors()).isNull()
        }

    @Test
    fun `GIVEN selected category with two sectors WHEN model created THEN the section leads with All`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val selection = CategorySelection(
            category = CategorySelection.Entry(id = stocks.id, displayName = stocks.displayName, code = stocks.code),
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
        assertThat(model.items().items.map { it.id }).containsExactly(umbrella.id, stocks.id)
    }

    @Test
    fun `GIVEN category with sector applied WHEN model created THEN Apply is hidden`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)

        // Act
        val model = createModel(testScope = this, selection = stocksWithTechnology)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().isApplyVisible).isFalse()
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
            assertThat(model.sectors()?.sectors?.map { it.label }).containsExactly(
                resourceReference(R.string.common_all),
                resourceReference(R.string.markets_real_assets_sector_xstocks_technology),
                resourceReference(R.string.markets_real_assets_sector_xstocks_healthcare),
            ).inOrder()
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
            userCountryResolver = userCountryResolver,
        )
    }

    private fun stubCategories(vararg categories: CoinCategory, country: String = "DE") {
        coEvery { getCoinCategoriesUseCase(country = country) } returns Either.Right(categories.toList())
    }

    private fun CategorySelectorModel.content() = uiState.value as CategorySelectorUM.Content

    private fun CategorySelectorModel.tapCategory(id: String) = items().items.single { it.id == id }.onItemClick()

    private fun CategorySelectorModel.tapSector(id: String) =
        requireNotNull(sectors()).sectors.single { it.id == id }.onItemClick()

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