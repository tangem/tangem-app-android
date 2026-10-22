package com.tangem.features.staking.impl.presentation.state.transformers.confirmation

import com.google.common.truth.Truth.assertThat
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetUpdatedAllowanceTransformerTest {

    private val transformer = SetUpdatedAllowanceTransformer(allowance = BigDecimal.TEN)

    @Test
    fun `GIVEN confirmation data WHEN allowance updated THEN confirmation keeps new allowance`() {
        // Arrange
        val confirmation = confirmationData(allowance = BigDecimal.ONE)
        val state = stakingUiState(confirmationState = confirmation)

        // Act
        val actual = transformer.transform(state)

        // Assert
        assertThat(actual).isEqualTo(state.copy(confirmationState = confirmation.copy(allowance = BigDecimal.TEN)))
    }

    @Test
    fun `GIVEN no confirmation data WHEN allowance updated THEN confirmation unchanged`() {
        // Arrange
        val state = stakingUiState(confirmationState = StakingStates.ConfirmationState.Empty())

        // Act
        val actual = transformer.transform(state)

        // Assert
        assertThat(actual).isEqualTo(state)
    }
}