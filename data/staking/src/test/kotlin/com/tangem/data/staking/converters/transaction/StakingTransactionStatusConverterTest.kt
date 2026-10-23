package com.tangem.data.staking.converters.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class StakingTransactionStatusConverterTest {

    private val converter = StakingTransactionStatusConverter()

    @ParameterizedTest
    @EnumSource(StakingTransactionStatusDTO::class)
    fun `GIVEN transaction status dto WHEN convert THEN domain constant has the same name`(
        dto: StakingTransactionStatusDTO,
    ) {
        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual.name).isEqualTo(dto.name)
    }

    @Test
    fun `GIVEN both enums WHEN compared THEN they have the same number of entries`() {
        // Act & Assert
        assertThat(StakingTransactionStatus.entries.size).isEqualTo(StakingTransactionStatusDTO.entries.size)
    }
}