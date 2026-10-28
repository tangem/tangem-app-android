package com.tangem.data.staking.converters.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionTypeDTO
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class StakingTransactionTypeConverterTest {

    private val converter = StakingTransactionTypeConverter()

    @ParameterizedTest
    @EnumSource(StakingTransactionTypeDTO::class)
    fun `GIVEN transaction type dto WHEN convert THEN domain constant has the same name`(
        dto: StakingTransactionTypeDTO,
    ) {
        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual.name).isEqualTo(dto.name)
    }

    @Test
    fun `GIVEN both enums WHEN compared THEN they have the same number of entries`() {
        // Act & Assert
        assertThat(StakingTransactionType.entries.size).isEqualTo(StakingTransactionTypeDTO.entries.size)
    }
}