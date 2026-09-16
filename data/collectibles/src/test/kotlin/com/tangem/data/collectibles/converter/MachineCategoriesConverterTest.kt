package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.MachineFactory
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import org.junit.jupiter.api.Test

internal class MachineCategoriesConverterTest {

    @Test
    fun `GIVEN a categories response WHEN convert THEN every field is mapped`() {
        // Arrange
        val response = MachineFactory.createCategoriesResponse(
            categories = listOf(
                MachineFactory.createCategoryDto(id = "pokemons", name = "Pokemons", machinesQuantity = 10),
                MachineFactory.createCategoryDto(id = "sports", name = "Sports", machinesQuantity = 37),
            ),
            totalMachinesQuantity = 47,
        )

        // Act
        val actual = MachineCategoriesConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            CollectibleGachaMachineCategories(
                categories = listOf(
                    CollectibleGachaMachineCategories.MachineCategory(
                        id = "pokemons",
                        name = "Pokemons",
                        machinesQuantity = 10,
                    ),
                    CollectibleGachaMachineCategories.MachineCategory(
                        id = "sports",
                        name = "Sports",
                        machinesQuantity = 37,
                    ),
                ),
                totalMachinesQuantity = 47,
            ),
        )
    }

    @Test
    fun `GIVEN no categories WHEN convert THEN the total is still carried over`() {
        // Arrange
        val response = MachineFactory.createCategoriesResponse(categories = emptyList(), totalMachinesQuantity = 47)

        // Act
        val actual = MachineCategoriesConverter.convert(response)

        // Assert
        assertThat(actual.categories).isEmpty()
        assertThat(actual.totalMachinesQuantity).isEqualTo(47)
    }
}