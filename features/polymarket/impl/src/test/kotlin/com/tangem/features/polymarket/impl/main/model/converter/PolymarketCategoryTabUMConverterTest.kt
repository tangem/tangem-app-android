package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCategory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCategoryTabUMConverterTest {

    private val clickedCategories = mutableListOf<Int>()

    private val converter = PolymarketCategoryTabUMConverter(onCategoryClick = { clickedCategories += it })

    // The class is PER_CLASS, so the recorded clicks would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        clickedCategories.clear()
    }

    @Test
    fun `GIVEN categories WHEN convert THEN a tab per category keeps the backend order`() {
        // Act
        val actual = converter.convert(input(selectedCategoryId = 1))

        // Assert
        assertThat(actual.map { it.id to it.label })
            .containsExactly(1 to "Trending", 2 to "Sport")
            .inOrder()
    }

    @Test
    fun `GIVEN a selected category WHEN convert THEN only its tab is selected`() {
        // Act
        val actual = converter.convert(input(selectedCategoryId = 2))

        // Assert
        assertThat(actual.map { it.id to it.isSelected })
            .containsExactly(1 to false, 2 to true)
            .inOrder()
    }

    @Test
    fun `GIVEN an unfiltered feed WHEN convert THEN no tab is selected`() {
        // Act
        val actual = converter.convert(input(selectedCategoryId = null))

        // Assert
        assertThat(actual.map { it.isSelected }).containsExactly(false, false)
    }

    @Test
    fun `GIVEN no categories WHEN convert THEN there are no tabs`() {
        // Act
        val actual = converter.convert(
            PolymarketCategoryTabUMConverter.Input(categories = emptyList(), selectedCategoryId = null),
        )

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN tabs WHEN one is clicked THEN its category id is reported`() {
        // Arrange
        val tabs = converter.convert(input(selectedCategoryId = 1))

        // Act
        tabs.single { it.id == 2 }.onClick()

        // Assert
        assertThat(clickedCategories).containsExactly(2)
    }

    private fun input(selectedCategoryId: Int?) = PolymarketCategoryTabUMConverter.Input(
        categories = listOf(
            PolymarketCategory(id = 1, label = "Trending", iconUrl = null),
            PolymarketCategory(id = 2, label = "Sport", iconUrl = null),
        ),
        selectedCategoryId = selectedCategoryId,
    )
}