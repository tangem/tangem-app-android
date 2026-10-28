package com.tangem.datasource.local.token.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import org.junit.jupiter.api.Test

internal class StakingActionTypeConverterTest {

    @Test
    fun `GIVEN both enums WHEN compared THEN they have the same number of entries`() {
        // Act & Assert
        assertThat(StakingActionType.entries.size).isEqualTo(StakingActionTypeDTO.entries.size)
    }
}