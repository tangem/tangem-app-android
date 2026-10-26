package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test

internal class DismissStakingNotificationsStateTransformerTest {

    private val approval = StakingNotification.Warning.TransactionInProgress.Approval
    private val first = StakingNotification.Info.Ordinary(stringReference("a"), stringReference("b"))
    private val second = StakingNotification.Info.Ordinary(stringReference("c"), stringReference("d"))

    @Test
    fun `GIVEN notifications of several classes WHEN dismiss class THEN only that class removed`() {
        // Act
        val actual = dismiss(
            StakingNotification.Warning.LowStakedBalance::class.java,
            StakingNotification.Warning.LowStakedBalance,
            approval,
            first,
        )

        // Assert
        assertThat(actual).containsExactly(approval, first).inOrder()
    }

    @Test
    fun `GIVEN two instances of class WHEN dismiss THEN all removed`() {
        // Act
        val actual = dismiss(StakingNotification.Info.Ordinary::class.java, first, second)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN parent class WHEN dismiss THEN subclasses kept`() {
        // Act
        val actual = dismiss(StakingNotification.Warning::class.java, StakingNotification.Warning.LowStakedBalance)

        // Assert
        assertThat(actual).containsExactly(StakingNotification.Warning.LowStakedBalance)
    }

    @Test
    fun `GIVEN empty confirmation WHEN dismiss THEN empty confirmation`() {
        // Act
        val actual = DismissStakingNotificationsStateTransformer(approval::class.java).transform(stakingUiState())

        // Assert
        assertThat(actual.confirmationState).isEqualTo(StakingStates.ConfirmationState.Empty())
    }

    private fun dismiss(type: Class<out NotificationUM>, vararg notifications: NotificationUM): List<NotificationUM> {
        val input = stakingUiState(
            confirmationState = confirmationData(notifications = notifications.toList().toPersistentList()),
        )
        return confirmation(DismissStakingNotificationsStateTransformer(type).transform(input)).notifications
    }

    private fun confirmation(state: StakingUiState) = state.confirmationState as StakingStates.ConfirmationState.Data
}