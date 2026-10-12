package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.MachineFactory
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MachineNftConverterTest {

    @Test
    fun `GIVEN insured value in cents WHEN convert THEN value is whole units`() {
        // Arrange
        val dto = MachineFactory.createNftDto(insuredValue = BigDecimal("240"))

        // Act
        val actual = MachineNftConverter.convert(dto)

        // Assert
        assertThat(actual.insuredValue).isEqualTo(BigDecimal("2.40"))
    }

    @Test
    fun `GIVEN a graded collectible WHEN convert THEN grader and grade are kept apart`() {
        // Arrange
        val dto = MachineFactory.createNftDto(gradingCompany = "PSA", gradeNum = "10")

        // Act
        val actual = MachineNftConverter.convert(dto)

        // Assert
        assertThat(actual.grade).isEqualTo(CollectibleGachaMachineNft.Grade(company = "PSA", value = "10"))
    }

    @Test
    fun `GIVEN no grading company WHEN convert THEN there is no grade`() {
        // Arrange
        val dto = MachineFactory.createNftDto(gradingCompany = null)

        // Act
        val actual = MachineNftConverter.convert(dto)

        // Assert
        assertThat(actual.grade).isNull()
    }

    @Test
    fun `GIVEN a blank grade WHEN convert THEN there is no grade`() {
        // Arrange
        val dto = MachineFactory.createNftDto(gradeNum = " ")

        // Act
        val actual = MachineNftConverter.convert(dto)

        // Assert
        assertThat(actual.grade).isNull()
    }

    @Test
    fun `GIVEN every optional field absent WHEN convert THEN the collectible still maps`() {
        // Arrange
        val dto = MachineFactory.createNftDto(
            description = null,
            rarity = null,
            gradingCompany = null,
            gradeNum = null,
            image = null,
            insuredValue = null,
        )

        // Act
        val actual = MachineNftConverter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(
            MachineFactory.createNft(
                description = null,
                rarity = null,
                grade = null,
                imageUrl = null,
                insuredValue = null,
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun rarity(model: RarityModel) {
        // Arrange
        val dto = MachineFactory.createNftDto(rarity = model.wire)

        // Act
        val actual = MachineNftConverter.convert(dto)

        // Assert
        assertThat(actual.rarity).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        RarityModel(wire = "common", expected = CollectibleRarity.COMMON),
        RarityModel(wire = "uncommon", expected = CollectibleRarity.UNCOMMON),
        RarityModel(wire = "rare", expected = CollectibleRarity.RARE),
        RarityModel(wire = "epic", expected = CollectibleRarity.EPIC),
        RarityModel(wire = "EPIC", expected = CollectibleRarity.EPIC),
        RarityModel(wire = "legendary", expected = null),
        RarityModel(wire = "", expected = null),
        RarityModel(wire = null, expected = null),
    )

    internal data class RarityModel(val wire: String?, val expected: CollectibleRarity?)
}