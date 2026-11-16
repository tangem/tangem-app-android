package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import com.tangem.grow.datasource.polymarket.models.PolymarketCategoryDto
import org.junit.jupiter.api.Test

internal class PolymarketCategoryConverterTest {

    @Test
    fun `GIVEN repeated children and parent id WHEN converted THEN first children retain provider order`() {
        // Arrange
        val category = category(id = 6).copy(
            subcategories = listOf(category(16), category(6), category(14), category(16)),
            filters = listOf("games", "future", "props"),
        )

        // Act
        val actual = PolymarketCategoryConverter.convert(category)

        // Assert
        assertThat(actual.subcategories.map { it.id }).containsExactly(16, 14).inOrder()
        assertThat(actual.subcategories.map { it.label })
            .containsExactly("category_16", "category_14").inOrder()
        assertThat(actual.filters).containsExactly(
            PolymarketCategoryFilter.Games,
            PolymarketCategoryFilter.Unknown("future"),
            PolymarketCategoryFilter.Props,
        ).inOrder()
    }

    @Test
    fun `GIVEN category without optional lists WHEN converted THEN it has no children or filters`() {
        // Act
        val actual = PolymarketCategoryConverter.convert(category())

        // Assert
        assertThat(actual.subcategories).isEmpty()
        assertThat(actual.filters).isEmpty()
    }

    private fun category(id: Int = 6) = PolymarketCategoryDto(id = id, label = "category_$id", icon = null)
}