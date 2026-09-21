package com.tangem.features.collectibles.impl.discovery.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.features.collectibles.impl.createCategories
import com.tangem.features.collectibles.impl.createCategory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesDiscoveryTabsConverterTest {

    private val clickedCategoryIds = mutableListOf<String?>()

    private val converter = CollectiblesDiscoveryTabsConverter(onCategoryClick = { clickedCategoryIds += it })

    // The class is PER_CLASS, so the recorded clicks would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        clickedCategoryIds.clear()
    }

    @Test
    fun `GIVEN backend categories WHEN convert THEN an All tab leads them in backend order`() {
        // Act
        val actual = converter.convert(input())

        // Assert
        assertThat(actual.map { it.id }).containsExactly("all", "sports", "anime").inOrder()
    }

    @Test
    fun `GIVEN categories WHEN convert THEN each tab counts its own machines`() {
        // Act
        val actual = converter.convert(input())

        // Assert
        assertThat(actual.filterIsInstance<TangemTabItemUM.Content>().map { it.counter })
            .containsExactly(stringReference("19"), stringReference("12"), stringReference("7"))
            .inOrder()
    }

    @Test
    fun `GIVEN no category selected WHEN convert THEN only the All tab is selected`() {
        // Act
        val actual = converter.convert(input(selectedCategoryId = null))

        // Assert
        assertThat(actual.selectedIds()).containsExactly("all")
    }

    @Test
    fun `GIVEN a selected category WHEN convert THEN only that tab is selected`() {
        // Act
        val actual = converter.convert(input(selectedCategoryId = "anime"))

        // Assert
        assertThat(actual.selectedIds()).containsExactly("anime")
    }

    @Test
    fun `GIVEN a selection the backend no longer offers WHEN convert THEN no tab is selected`() {
        // Act
        val actual = converter.convert(input(selectedCategoryId = "retired"))

        // Assert
        assertThat(actual.selectedIds()).isEmpty()
    }

    @Test
    fun `GIVEN no backend categories WHEN convert THEN only the All tab is built`() {
        // Act
        val actual = converter.convert(
            input(categories = createCategories(categories = emptyList(), totalMachinesQuantity = 0)),
        )

        // Assert
        assertThat(actual.map { it.id }).containsExactly("all")
    }

    @Test
    fun `GIVEN the All tab WHEN it is clicked THEN an unfiltered catalogue is requested`() {
        // Arrange
        val tabs = converter.convert(input(selectedCategoryId = "anime"))

        // Act
        tabs.contentTab(id = "all").onClick()

        // Assert
        assertThat(clickedCategoryIds).containsExactly(null)
    }

    @Test
    fun `GIVEN a category tab WHEN it is clicked THEN its id is reported`() {
        // Arrange
        val tabs = converter.convert(input())

        // Act
        tabs.contentTab(id = "sports").onClick()

        // Assert
        assertThat(clickedCategoryIds).containsExactly("sports")
    }

    private fun input(
        categories: CollectibleGachaMachineCategories = createCategories(
            categories = listOf(
                createCategory(id = "sports", name = "Sports", machinesQuantity = 12),
                createCategory(id = "anime", name = "Anime", machinesQuantity = 7),
            ),
            totalMachinesQuantity = 19,
        ),
        selectedCategoryId: String? = null,
    ) = CollectiblesDiscoveryTabsConverter.Input(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
    )

    private fun List<TangemTabItemUM>.selectedIds(): List<String> =
        filterIsInstance<TangemTabItemUM.Content>().filter { it.isSelected }.map { it.id }

    private fun List<TangemTabItemUM>.contentTab(id: String): TangemTabItemUM.Content =
        filterIsInstance<TangemTabItemUM.Content>().single { it.id == id }
}