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

/** The umbrella id must never travel in a selection, and Featured is not a filter at all. */
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
            CategorySelection(
                category = CategorySelection.Entry(
                    id = bonds.id,
                    displayName = bonds.displayName,
                    code = bonds.code,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN selected category with a lone sector WHEN model created THEN the sectors section is hidden`() =
        runTest {
            // Arrange
            stubCategories(umbrella, bonds)
            val selection = CategorySelection(
                category = CategorySelection.Entry(
                    id = bonds.id,
                    displayName = bonds.displayName,
                    code = bonds.code,
                ),
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
    fun `GIVEN an unrestricted region WHEN reset tapped THEN the filter is cleared`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks, commodities)
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()
        // the footer carrying Reset only appears once something is edited, so take the route a user has
        model.items().items.first { it.id == umbrella.id }.onItemClick()

        // Act
        model.content().footer.onReset()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(CategorySelection())
    }

    @Test
    fun `GIVEN a restricted region WHEN reset tapped THEN the regional default comes back`() = runTest {
        // Arrange
        coEvery { userCountryResolver.resolve() } returns "US"
        stubCategories(umbrella, stocks, commodities, country = "US")
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = stocksWithTechnology, callback = callback)
        advanceUntilIdle()
        model.items().items.first { it.id == umbrella.id }.onItemClick()

        // Act
        model.content().footer.onReset()

        // Assert — the reset differs from what the screen opened on, so it is there to be applied
        assertThat(model.content().isEdited).isTrue()

        // Act
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(commoditiesSelection)
    }

    @Test
    fun `GIVEN a restricted region opened on its default WHEN reset tapped THEN the footer is hidden`() = runTest {
        // Arrange
        coEvery { userCountryResolver.resolve() } returns "US"
        stubCategories(umbrella, stocks, commodities, country = "US")
        val model = createModel(testScope = this, selection = commoditiesSelection)
        advanceUntilIdle()
        model.items().items.first { it.id == stocks.id }.onItemClick()

        // Act
        model.content().footer.onReset()

        // Assert
        assertThat(model.content().isEdited).isFalse()
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

    private fun stubCategories(vararg categories: CoinCategory, country: String = "DE") {
        coEvery { getCoinCategoriesUseCase(country = country) } returns Either.Right(categories.toList())
    }

    @Test
    fun `GIVEN no filter WHEN model created THEN the footer is hidden`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().isEdited).isFalse()
    }

    @Test
    fun `GIVEN no filter WHEN another category tapped THEN the footer is shown`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.items().items.first { it.id == stocks.id }.onItemClick()

        // Assert
        assertThat(model.content().isEdited).isTrue()
    }

    @Test
    fun `GIVEN an edited selection WHEN reset tapped THEN the footer is hidden again`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.items().items.first { it.id == stocks.id }.onItemClick()

        // Act
        model.content().footer.onReset()

        // Assert
        assertThat(model.content().isEdited).isFalse()
    }

    @Test
    fun `GIVEN applied category WHEN the same category tapped THEN the footer stays hidden`() = runTest {
        // Arrange
        stubCategories(umbrella, stocks)
        val model = createModel(testScope = this, selection = stocksWithTechnology)
        advanceUntilIdle()

        // Act
        model.items().items.first { it.id == stocks.id }.onItemClick()

        // Assert
        assertThat(model.content().isEdited).isFalse()
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