package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.Test

internal class SetConfirmationStateInProgressTransformerTest {

    private val transformer = SetConfirmationStateInProgressTransformer()

    @Test
    fun `GIVEN assent confirmation WHEN transform THEN in progress with primary button disabled`() {
        // Arrange
        val confirmation = confirmationData(isPrimaryButtonEnabled = true)
        val state = stakingUiState(confirmationState = confirmation)

        // Act
        val actual = transformer.transform(state)

        // Assert
        assertThat(actual).isEqualTo(
            state.copy(
                confirmationState = confirmation.copy(
                    isPrimaryButtonEnabled = false,
                    innerState = InnerConfirmationStakingState.IN_PROGRESS,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN no confirmation data WHEN transform THEN confirmation unchanged`() {
        // Arrange
        val state = stakingUiState()

        // Act
        val actual = transformer.transform(state)

        // Assert
        assertThat(actual).isEqualTo(state)
    }
}