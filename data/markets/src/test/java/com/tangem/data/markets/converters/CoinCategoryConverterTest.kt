package com.tangem.data.markets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.CoinCategory
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse.Category
import org.junit.jupiter.api.Test

internal class CoinCategoryConverterTest {

    @Test
    fun `GIVEN category with a sector WHEN convert THEN sector code and display order are carried over`() {
        // Arrange
        val value = category(sectors = listOf(sector(id = "1", code = "technology", displayOrder = 3)))

        // Act
        val actual = CoinCategoryConverter.convert(value)

        // Assert
        assertThat(actual).isEqualTo(
            CoinCategory(
                id = "42",
                code = "stocks",
                displayName = "Stocks",
                displayOrder = 1,
                tokensCount = 5,
                isRestricted = false,
                isVisible = true,
                sectors = listOf(
                    CoinCategory.Sector(
                        id = "1",
                        code = "technology",
                        displayName = "Technology",
                        displayOrder = 3,
                        tokensCount = 7,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN sectors out of order WHEN convert THEN they follow display order`() {
        // Arrange
        val value = category(
            sectors = listOf(
                sector(id = "3", code = "healthcare", displayOrder = 2),
                sector(id = "1", code = "technology", displayOrder = 0),
                sector(id = "2", code = "financials", displayOrder = 1),
            ),
        )

        // Act
        val actual = CoinCategoryConverter.convert(value)

        // Assert
        assertThat(actual.sectors.map { it.code }).containsExactly("technology", "financials", "healthcare").inOrder()
    }

    private fun category(sectors: List<Category.Sector>) = Category(
        id = "42",
        code = "stocks",
        displayName = "Stocks",
        displayOrder = 1,
        tokensCount = 5,
        isRestricted = false,
        isVisible = true,
        sectors = sectors,
    )

    private fun sector(id: String, code: String, displayOrder: Int) = Category.Sector(
        id = id,
        code = code,
        displayName = code.replaceFirstChar(Char::uppercase),
        displayOrder = displayOrder,
        tokensCount = 7,
    )
}