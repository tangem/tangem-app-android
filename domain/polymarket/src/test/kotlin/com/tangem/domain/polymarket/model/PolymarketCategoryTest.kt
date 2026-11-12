package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter.Games
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter.Props
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter.Unknown
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCategoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN server filters WHEN selectableFilters THEN known filters kept only when at least two`(
        model: SelectableFiltersModel,
    ) {
        // Arrange
        val category = PolymarketCategory(id = 1, label = "Sports", icon = null, filters = model.filters)

        // Act
        val actual = category.selectableFilters

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    private fun provideTestModels() = listOf(
        SelectableFiltersModel(filters = emptyList(), expected = emptyList()),
        SelectableFiltersModel(filters = listOf(Games), expected = emptyList()),
        SelectableFiltersModel(filters = listOf(Games, Unknown("future")), expected = emptyList()),
        SelectableFiltersModel(filters = listOf(Props, Games), expected = listOf(Props, Games)),
        SelectableFiltersModel(filters = listOf(Games, Unknown("future"), Props), expected = listOf(Games, Props)),
    )

    data class SelectableFiltersModel(
        val filters: List<PolymarketCategoryFilter>,
        val expected: List<PolymarketCategoryFilter>,
    )
}