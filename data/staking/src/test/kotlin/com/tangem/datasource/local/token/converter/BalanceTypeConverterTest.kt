package com.tangem.datasource.local.token.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.BalanceType
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO.BalanceTypeDTO
import org.junit.jupiter.api.Test

internal class BalanceTypeConverterTest {

    @Test
    fun `GIVEN both enums WHEN compared THEN they have the same number of entries`() {
        // Act & Assert
        assertThat(BalanceType.entries.size).isEqualTo(BalanceTypeDTO.entries.size)
    }
}