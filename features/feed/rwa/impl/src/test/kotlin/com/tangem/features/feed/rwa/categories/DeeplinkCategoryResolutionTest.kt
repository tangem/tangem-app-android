package com.tangem.features.feed.rwa.categories

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.CoinCategory
import org.junit.jupiter.api.Test

/**
 * A deeplink can carry either identifier the backend knows a category by, and must never select
 * something the Category selector itself does not offer.
 */
internal class DeeplinkCategoryResolutionTest {

    private val stocks = category(id = "1", code = "stocks", displayName = "Stocks")
    private val commodities = category(id = "2", code = "commodities", displayName = "Commodities")
    private val umbrella = category(id = "3", code = ReservedCategoryCodes.REAL_WORLD_ASSETS, displayName = "All")
    private val featured = category(id = "4", code = ReservedCategoryCodes.FEATURED_ASSETS, displayName = "Featured")
    private val hidden = category(id = "5", code = "bonds", displayName = "Bonds", isVisible = false)

    private val categories = listOf(stocks, commodities, umbrella, featured, hidden)

    @Test
    fun `GIVEN an id WHEN resolved THEN that category is selected`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = "2")

        // Assert
        assertThat(selection?.category)
            .isEqualTo(CategorySelection.Entry(id = "2", displayName = "Commodities", code = "commodities"))
    }

    @Test
    fun `GIVEN a code WHEN resolved THEN that category is selected`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = "stocks")

        // Assert
        assertThat(selection?.category)
            .isEqualTo(CategorySelection.Entry(id = "1", displayName = "Stocks", code = "stocks"))
    }

    @Test
    fun `GIVEN a value that is one category's id and another's code WHEN resolved THEN the id wins`() {
        // Arrange — "stocks" is the code of the first and the id of the second
        val ambiguous = listOf(stocks, category(id = "stocks", code = "bonds", displayName = "Bonds"))

        // Act
        val selection = ambiguous.resolveDeeplinkSelection(categoryId = "stocks")

        // Assert
        assertThat(selection?.category?.displayName).isEqualTo("Bonds")
    }

    @Test
    fun `GIVEN the umbrella WHEN resolved THEN nothing is filtered because it is All`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = ReservedCategoryCodes.REAL_WORLD_ASSETS)

        // Assert
        assertThat(selection).isEqualTo(CategorySelection())
    }

    @Test
    fun `GIVEN the featured rail's code WHEN resolved THEN it is not selectable`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = ReservedCategoryCodes.FEATURED_ASSETS)

        // Assert
        assertThat(selection).isNull()
    }

    @Test
    fun `GIVEN a hidden category WHEN resolved THEN it is not selectable`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = "bonds")

        // Assert
        assertThat(selection).isNull()
    }

    @Test
    fun `GIVEN an unknown value WHEN resolved THEN there is nothing to apply`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = "made-up")

        // Assert
        assertThat(selection).isNull()
    }

    @Test
    fun `GIVEN no value WHEN resolved THEN there is nothing to apply`() {
        // Act
        val selection = categories.resolveDeeplinkSelection(categoryId = null)

        // Assert
        assertThat(selection).isNull()
    }

    private fun category(id: String, code: String, displayName: String, isVisible: Boolean = true) = CoinCategory(
        id = id,
        code = code,
        displayName = displayName,
        displayOrder = 1,
        tokensCount = 1,
        isRestricted = false,
        sectors = emptyList(),
        isVisible = isVisible,
    )
}