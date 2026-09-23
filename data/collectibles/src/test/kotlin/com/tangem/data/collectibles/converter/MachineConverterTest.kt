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
        val dto = MachineFactory.createDto(expectedValue = expectedValue)

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.expectedValue).isEqualTo(expectedValue)
        assertThat(actual.expectedValue.scale()).isEqualTo(expectedValue.scale())
    }

    @Test
    fun `GIVEN money in cents WHEN convert THEN only the price and fee fields become whole units`() {
        // Arrange
        val dto = MachineFactory.createDto()

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.price).isEqualTo(BigDecimal("50.00"))
        assertThat(actual.fee).isEqualTo(BigDecimal("2.00"))
        assertThat(actual.feeAmount).isEqualTo(BigDecimal("1.00"))
        assertThat(actual.expectedValue).isEqualTo(dto.expectedValue)
    }

    @Test
    fun `GIVEN nested tier data WHEN convert THEN odds stock and tier ranges are mapped per rarity`() {
        // Arrange
        val dto = MachineFactory.createDto()

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.odds).isEqualTo(
            CollectibleGachaMachine.Odds(common = 80, uncommon = 15, rare = 4, epic = 1),
        )
        assertThat(actual.stock).isEqualTo(
            CollectibleGachaMachine.Stock(common = 1949, uncommon = 3225, rare = 2375, epic = 2628),
        )
        assertThat(actual.tierRanges).isEqualTo(
            CollectibleGachaMachine.TierRanges(
                common = CollectibleGachaMachine.TierRange(start = 30, end = 60),
                uncommon = CollectibleGachaMachine.TierRange(start = 60, end = 110),
                rare = CollectibleGachaMachine.TierRange(start = 110, end = 250),
                epic = CollectibleGachaMachine.TierRange(start = 250, end = 5001),
            ),
        )
    }

    @Test
    fun `GIVEN a dto without an epic tier WHEN convert THEN the epic fields are null`() {
        // Arrange
        val dto = MachineFactory.createDto(epicOdds = null, epicStock = null, epicRange = null)

        // Act
        val actual = MachineConverter.convert(dto)

        // Assert
        assertThat(actual.odds.epic).isNull()
        assertThat(actual.stock.epic).isNull()
        assertThat(actual.tierRanges.epic).isNull()
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