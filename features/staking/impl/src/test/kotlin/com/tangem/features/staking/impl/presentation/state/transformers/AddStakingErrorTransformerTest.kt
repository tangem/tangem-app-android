package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.feeContent
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.Test

internal class AddStakingErrorTransformerTest {

    @Test
    fun `GIVEN confirmation data WHEN staking error added THEN fee state error`() {
        // Arrange
        val confirmation = confirmationData(feeState = feeContent())
        val state = stakingUiState(confirmationState = confirmation)

        // Act
        val actual = AddStakingErrorTransformer.transform(state)

        // Assert
        assertThat(actual).isEqualTo(state.copy(confirmationState = confirmation.copy(feeState = FeeState.Error)))
    }

    @Test
    fun `GIVEN no confirmation data WHEN staking error added THEN same state`() {
        // Arrange
        val state = stakingUiState()

        // Act
        val actual = AddStakingErrorTransformer.transform(state)

        // Assert
        assertThat(actual).isSameInstanceAs(state)
    }
}