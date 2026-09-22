package com.tangem.features.collectibles.impl.packdetails.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.machinedetails.model.converter.CollectiblesMachineDetailRowsConverter
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import org.junit.jupiter.api.Test

internal class CollectiblesMachineDetailRowsConverterTest {

    @Test
    fun `GIVEN a pack of several cards WHEN convert THEN the count is pluralized`() {
        // Arrange
        val machine = createMachine(contains = 3, instantBuybackPercent = 85)

        // Act
        val actual = CollectiblesMachineDetailRowsConverter.convert(machine)

        // Assert
        assertThat(actual).containsExactly(
            CollectiblesMachineDetailsUM.DetailUM(
                id = "contains",
                label = stringReference("Pack contains"),
                value = stringReference("3 cards"),
            ),
            CollectiblesMachineDetailsUM.DetailUM(
                id = "buyback",
                label = stringReference("Instant buyback"),
                value = stringReference("85% of value"),
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN a pack of one card WHEN convert THEN the count is singular`() {
        // Arrange
        val machine = createMachine(contains = 1)

        // Act
        val actual = CollectiblesMachineDetailRowsConverter.convert(machine)

        // Assert
        assertThat(actual.first().value).isEqualTo(stringReference("1 card"))
    }

    @Test
    fun `GIVEN an unknown card count WHEN convert THEN only the buyback card is built`() {
        // Arrange
        val machine = createMachine(contains = null)

        // Act
        val actual = CollectiblesMachineDetailRowsConverter.convert(machine)

        // Assert
        assertThat(actual.map { it.id }).containsExactly("buyback")
    }
}