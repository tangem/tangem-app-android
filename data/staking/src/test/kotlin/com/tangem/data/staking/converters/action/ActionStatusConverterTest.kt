package com.tangem.data.staking.converters.action

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionStatusDTO
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class ActionStatusConverterTest {

    private val converter = ActionStatusConverter()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @EnumSource(StakingActionStatusDTO::class)
        fun `GIVEN action status dto WHEN convert THEN domain constant has the same name`(dto: StakingActionStatusDTO) {
            // Act
            val actual = converter.convert(dto)

            // Assert
            assertThat(actual.name).isEqualTo(dto.name)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @EnumSource(StakingActionStatus::class)
        fun `GIVEN domain action status WHEN convertBack THEN dto constant has the same name`(
            domain: StakingActionStatus,
        ) {
            // Act
            val actual = converter.convertBack(domain)

            // Assert
            assertThat(actual.name).isEqualTo(domain.name)
        }
    }
}