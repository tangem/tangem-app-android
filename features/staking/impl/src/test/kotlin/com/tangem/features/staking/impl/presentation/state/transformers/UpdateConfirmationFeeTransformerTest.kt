package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.feeContent
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class UpdateConfirmationFeeTransformerTest {

    private val transformer = UpdateConfirmationFeeTransformer(newFee = ethFee(BigDecimal("0.02")))

    @Test
    fun `GIVEN content fee WHEN transform THEN only fee replaced`() {
        // Arrange
        val input = stakingUiState(
            confirmationState = confirmationData(
                feeState = feeContent(ethFee(BigDecimal("0.01")), isFeeApproximate = true),
            ),
        )

        // Act
        val actual = transformer.transform(input).confirmationState as StakingStates.ConfirmationState.Data

        // Assert
        assertThat(actual.feeState).isEqualTo(feeContent(ethFee(BigDecimal("0.02")), isFeeApproximate = true))
    }

    @Test
    fun `GIVEN loading fee WHEN transform THEN same state`() {
        // Arrange
        val input = stakingUiState(confirmationState = confirmationData(feeState = FeeState.Loading))

        // Act & Assert
        assertThat(transformer.transform(input)).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN empty confirmation WHEN transform THEN same state`() {
        // Arrange
        val input = stakingUiState()

        // Act & Assert
        assertThat(transformer.transform(input)).isSameInstanceAs(input)
    }
}