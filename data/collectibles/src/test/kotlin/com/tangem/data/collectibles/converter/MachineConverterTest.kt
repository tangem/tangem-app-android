package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.MachineFactory
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MachineConverterTest {

    @Test
    fun `GIVEN a full dto WHEN convert THEN every field is mapped`() {
        // Arrange
        val dto = MachineFactory.createDto()

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(MachineFactory.create())
    }

    @Test
    fun `GIVEN a dto WHEN convert THEN the image field becomes imageUrl`() {
        // Arrange
        val dto = MachineFactory.createDto(image = "https://cdn/pack.png")

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.imageUrl).isEqualTo("https://cdn/pack.png")
    }

    @Test
    fun `GIVEN a dto without an image WHEN convert THEN imageUrl is null`() {
        // Arrange
        val dto = MachineFactory.createDto(image = null)

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.imageUrl).isNull()
    }

    @Test
    fun `GIVEN a dto without a contains count WHEN convert THEN contains is null`() {
        // Arrange
        val dto = MachineFactory.createDto(contains = null)

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.contains).isNull()
    }

    @Test
    fun `GIVEN dtos differing only in timestamps WHEN convert THEN the machines are equal`() {
        // Arrange — createdAt and updatedAt are deliberately not carried into the domain model
        val early = MachineFactory.createDto(createdAt = "2024-01-01 00:00:00.000 +0000", updatedAt = "2024-01-01")
        val late = MachineFactory.createDto(createdAt = "2026-08-28 12:00:00.000 +0000", updatedAt = "2026-08-28")

        // Act
        val actual = MachineConverter.convert(early)
        val other = MachineConverter.convert(late)

        // Assert
        assertThat(actual).isEqualTo(other)
    }

    @Test
    fun `GIVEN a high precision expected value WHEN convert THEN the scale is preserved`() {
        // Arrange
        val expectedValue = BigDecimal("54.578583779326468")
        val dto = MachineFactory.createDto(price = BigDecimal("0.000000001"), expectedValue = expectedValue)

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.expectedValue).isEqualTo(expectedValue)
        assertThat(actual.expectedValue.scale()).isEqualTo(expectedValue.scale())
        assertThat(actual.price).isEqualTo(BigDecimal("0.000000001"))
    }

    @Test
    fun `GIVEN nested tier data WHEN convert THEN odds stock and tier ranges are mapped per rarity`() {
        // Arrange
        val dto = MachineFactory.createDto()

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.odds).isEqualTo(
            CollectibleGachaMachine.Odds(common = 80, rare = 15, epic = 4, legendary = 1),
        )
        assertThat(actual.stock).isEqualTo(
            CollectibleGachaMachine.Stock(common = 1949, rare = 3225, epic = 2375, legendary = 2628),
        )
        assertThat(actual.tierRanges).isEqualTo(
            CollectibleGachaMachine.TierRanges(
                common = CollectibleGachaMachine.TierRange(start = 30, end = 60),
                rare = CollectibleGachaMachine.TierRange(start = 60, end = 110),
                epic = CollectibleGachaMachine.TierRange(start = 110, end = 250),
                legendary = CollectibleGachaMachine.TierRange(start = 250, end = 5001),
            ),
        )
    }

    @Test
    fun `GIVEN a dto without a legendary tier WHEN convert THEN the legendary fields are null`() {
        // Arrange
        val dto = MachineFactory.createDto(legendaryOdds = null, legendaryStock = null, legendaryRange = null)

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.odds.legendary).isNull()
        assertThat(actual.stock.legendary).isNull()
        assertThat(actual.tierRanges.legendary).isNull()
    }

    @Test
    fun `GIVEN several dtos WHEN convertList THEN order is preserved`() {
        // Arrange
        val dtos = listOf(MachineFactory.createDto(id = "a"), MachineFactory.createDto(id = "b"))

        // Act
        val actual = MachineConverter.convertList(dtos)

        // Assert
        assertThat(actual.map { it.id }).containsExactly("a", "b").inOrder()
    }
}