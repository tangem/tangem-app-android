package com.tangem.features.feed.rwa.categories

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.CoinCategory
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RwaDefaultCategoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) {
        // Act
        val actual = RwaDefaultCategory.resolve(
            categories = model.categories,
            country = model.country,
            hasUserSelection = model.hasUserSelection,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ResolveModel(
        val country: String?,
        val categories: List<CoinCategory>,
        val expected: CategorySelection?,
        val hasUserSelection: Boolean = false,
    )

    private fun provideTestModels() = listOf(
        ResolveModel(country = "US", categories = allCategories, expected = commoditiesSelection),
        ResolveModel(country = "GB", categories = allCategories, expected = commoditiesSelection),
        ResolveModel(country = "DE", categories = allCategories, expected = null),
        ResolveModel(country = null, categories = allCategories, expected = null),
        // the backend may retire the category — anything is a safer scope than the umbrella here
        ResolveModel(
            country = "US",
            categories = listOf(umbrella, bonds, stocks),
            expected = stocksSelection,
        ),
        ResolveModel(
            country = "GB",
            categories = listOf(featured, umbrella, hiddenEquities, bonds, stocks),
            expected = stocksSelection,
        ),
        ResolveModel(country = "US", categories = listOf(umbrella), expected = null),
        // an invisible commodities is a category the selector never offers, so it cannot be the default
        ResolveModel(
            country = "US",
            categories = listOf(umbrella, hiddenCommodities, stocks),
            expected = stocksSelection,
        ),
        ResolveModel(country = "US", categories = listOf(featured, umbrella, hiddenEquities), expected = null),
        // a blocked commodities is no better a landing scope than any other blocked category
        ResolveModel(
            country = "US",
            categories = listOf(umbrella, restrictedCommodities, bonds),
            expected = bondsSelection,
        ),
        // the whole point of the default is to open on something the region allows, so a restricted
        // category loses to an allowed one even when it sorts first
        ResolveModel(
            country = "US",
            categories = listOf(umbrella, restrictedStocks, bonds),
            expected = bondsSelection,
        ),
        // nothing allowed anywhere: a restricted category still beats the umbrella, which mixes everything
        ResolveModel(
            country = "US",
            categories = listOf(umbrella, restrictedStocks),
            expected = restrictedStocksSelection,
        ),
        // a pick made before the first successful load must never be replaced by the default
        ResolveModel(country = "US", categories = allCategories, expected = null, hasUserSelection = true),
        ResolveModel(country = "GB", categories = allCategories, expected = null, hasUserSelection = true),
    )

    private companion object {

        val featured = category(id = "0", code = "featured-assets", displayName = "Featured", displayOrder = 0)
        val umbrella = category(id = "1", code = "real-world-assets", displayName = "Real Assets")
        val commodities = category(id = "4", code = "commodities", displayName = "Commodities")
        val stocks = category(id = "5", code = "stocks", displayName = "Stocks", displayOrder = 1)
        val bonds = category(id = "6", code = "bonds", displayName = "Bonds", displayOrder = 2)
        val restrictedCommodities = category(
            id = "10",
            code = "commodities",
            displayName = "Commodities",
            displayOrder = 0,
            isRestricted = true,
        )
        val restrictedStocks = category(
            id = "9",
            code = "stocks",
            displayName = "Stocks",
            displayOrder = 1,
            isRestricted = true,
        )
        val restrictedStocksSelection = CategorySelection(
            category = CategorySelection.Entry(id = "9", displayName = "Stocks", code = "stocks"),
        )
        val bondsSelection = CategorySelection(
            category = CategorySelection.Entry(id = "6", displayName = "Bonds", code = "bonds"),
        )
        val hiddenCommodities = category(
            id = "8",
            code = "commodities",
            displayName = "Commodities",
            displayOrder = 0,
            isVisible = false,
        )
        val hiddenEquities =
            category(id = "7", code = "equities", displayName = "Equities", displayOrder = 1, isVisible = false)
        val allCategories = listOf(umbrella, commodities)

        val commoditiesSelection = CategorySelection(
            category = CategorySelection.Entry(id = "4", displayName = "Commodities", code = "commodities"),
        )
        val stocksSelection = CategorySelection(
            category = CategorySelection.Entry(id = "5", displayName = "Stocks", code = "stocks"),
        )

        fun category(
            id: String,
            code: String,
            displayName: String,
            displayOrder: Int = 0,
            isVisible: Boolean = true,
            isRestricted: Boolean = false,
        ) = CoinCategory(
            id = id,
            code = code,
            displayName = displayName,
            displayOrder = displayOrder,
            tokensCount = 17,
            isRestricted = isRestricted,
            sectors = emptyList(),
            isVisible = isVisible,
        )
    }
}