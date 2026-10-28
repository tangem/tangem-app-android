package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.google.common.truth.Truth.assertThat
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class SetApprovalInProgressTransformerTest {

    @Test
    fun `GIVEN data with notifications WHEN transform THEN approval notification appended and disabled`() {
        // Arrange
        val input = stakingUiState(
            confirmationState = confirmationData(
                notifications = persistentListOf(StakingNotification.Warning.LowStakedBalance),
                isPrimaryButtonEnabled = true,
            ),
        )

        // Act
        val actual = SetApprovalInProgressTransformer.transform(input).confirmationState
            as StakingStates.ConfirmationState.Data

        // Assert
        assertThat(actual.notifications).containsExactly(
            StakingNotification.Warning.LowStakedBalance,
            StakingNotification.Warning.TransactionInProgress.Approval,
        ).inOrder()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN empty confirmation WHEN transform THEN unchanged`() {
        // Act
        val actual = SetApprovalInProgressTransformer.transform(stakingUiState())

        // Assert
        assertThat(actual.confirmationState).isEqualTo(StakingStates.ConfirmationState.Empty())
    }
}