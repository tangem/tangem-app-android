package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.grow.datasource.collectibles.models.request.GenerateTxRequestBody.TurboModeDTO
import org.junit.jupiter.api.Test

internal class TurboModeConverterTest {

    @Test
    fun `GIVEN an enabled turbo mode WHEN convertBack THEN the dto carries both fields`() {
        // Arrange
        val model = CollectibleTurboMode(isEnabled = true, buybackAddress = "0xbuyback")

        // Act
        val actual = TurboModeConverter.convertBack(model)

        // Assert
        assertThat(actual).isEqualTo(TurboModeDTO(isEnabled = true, buybackAddress = "0xbuyback"))
    }

    @Test
    fun `GIVEN no buyback address WHEN convertBack THEN the dto keeps it null`() {
        // Arrange
        val model = CollectibleTurboMode(isEnabled = false)

        // Act
        val actual = TurboModeConverter.convertBack(model)

        // Assert
        assertThat(actual).isEqualTo(TurboModeDTO(isEnabled = false, buybackAddress = null))
    }

    @Test
    fun `GIVEN a turbo mode dto WHEN convert THEN it maps back to the domain model`() {
        // Arrange
        val dto = TurboModeDTO(isEnabled = true, buybackAddress = "0xbuyback")

        // Act
        val actual = TurboModeConverter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(CollectibleTurboMode(isEnabled = true, buybackAddress = "0xbuyback"))
    }

    @Test
    fun `GIVEN a default dto WHEN convert THEN turbo mode is disabled without a buyback address`() {
        // Arrange
        val dto = TurboModeDTO()

        // Act
        val actual = TurboModeConverter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(CollectibleTurboMode(isEnabled = false, buybackAddress = null))
    }

    @Test
    fun `GIVEN a turbo mode WHEN converted back and forth THEN it round trips`() {
        // Arrange
        val model = CollectibleTurboMode(isEnabled = true, buybackAddress = "0xbuyback")

        // Act
        val actual = TurboModeConverter.convert(TurboModeConverter.convertBack(model))

        // Assert
        assertThat(actual).isEqualTo(model)
    }
}