package com.tangem.features.collectibles.impl.packdetails.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.features.collectibles.impl.createNft
import com.tangem.features.collectibles.impl.machinedetails.model.converter.CollectiblesPrizeConverter
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CollectiblesPrizeConverterTest {

    @Test
    fun `GIVEN a graded collectible WHEN convert THEN the badge joins grader and grade`() {
        // Arrange
        val nft = createNft(grade = CollectibleGachaMachineNft.Grade(company = "PSA", value = "10"))

        // Act
        val actual = CollectiblesPrizeConverter.convert(nft)

        // Assert
        assertThat(actual.grade).isEqualTo(stringReference("PSA10"))
    }

    @Test
    fun `GIVEN an ungraded collectible WHEN convert THEN there is no badge`() {
        // Arrange
        val nft = createNft(grade = null)

        // Act
        val actual = CollectiblesPrizeConverter.convert(nft)

        // Assert
        assertThat(actual.grade).isNull()
    }

    @Test
    fun `GIVEN an insured value WHEN convert THEN it is grouped and dropped of trailing zeros`() {
        // Arrange
        val nft = createNft(insuredValue = BigDecimal("4000"))

        // Act
        val actual = CollectiblesPrizeConverter.convert(nft)

        // Assert
        assertThat(actual.value).isEqualTo(stringReference("$4,000"))
    }

    @Test
    fun `GIVEN no insured value WHEN convert THEN the value falls back to a dash`() {
        // Arrange
        val nft = createNft(insuredValue = null)

        // Act
        val actual = CollectiblesPrizeConverter.convert(nft)

        // Assert
        assertThat(actual.value).isEqualTo(stringReference("—"))
    }
}