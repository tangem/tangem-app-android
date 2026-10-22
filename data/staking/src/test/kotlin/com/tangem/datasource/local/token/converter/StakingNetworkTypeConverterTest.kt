package com.tangem.datasource.local.token.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.NetworkType
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class StakingNetworkTypeConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @EnumSource(NetworkTypeDTO::class)
        fun `GIVEN network dto WHEN convert THEN domain constant has the same name`(dto: NetworkTypeDTO) {
            // Act
            val actual = StakingNetworkTypeConverter.convert(dto)

            // Assert
            assertThat(actual.name).isEqualTo(dto.name)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @EnumSource(NetworkType::class)
        fun `GIVEN domain network WHEN convertBack THEN dto constant has the same name`(domain: NetworkType) {
            // Act
            val actual = StakingNetworkTypeConverter.convertBack(domain)

            // Assert
            assertThat(actual.name).isEqualTo(domain.name)
        }
    }

    @Test
    fun `GIVEN both enums WHEN compared THEN they have the same number of entries`() {
        // Act & Assert
        assertThat(NetworkType.entries.size).isEqualTo(NetworkTypeDTO.entries.size)
    }
}