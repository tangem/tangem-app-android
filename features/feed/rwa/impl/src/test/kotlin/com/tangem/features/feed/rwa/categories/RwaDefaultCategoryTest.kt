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
        // the backend may retire the category; the umbrella is then the only sane scope left
        ResolveModel(country = "US", categories = listOf(umbrella), expected = null),
        // a pick made before the first successful load must never be replaced by the default
        ResolveModel(country = "US", categories = allCategories, expected = null, hasUserSelection = true),
        ResolveModel(country = "GB", categories = allCategories, expected = null, hasUserSelection = true),
    )

    private companion object {

        val umbrella = category(id = "1", code = "real-world-assets", displayName = "Real Assets")
        val commodities = category(id = "4", code = "commodities", displayName = "Commodities")
        val allCategories = listOf(umbrella, commodities)

        val commoditiesSelection = CategorySelection(
            category = CategorySelection.Entry(id = "4", displayName = "Commodities", code = "commodities"),
        )

        fun category(id: String, code: String, displayName: String) = CoinCategory(
            id = id,
            code = code,
            displayName = displayName,
            displayOrder = 0,
            tokensCount = 17,
            isRestricted = false,
            sectors = emptyList(),
            isVisible = true,
        )
    }
}