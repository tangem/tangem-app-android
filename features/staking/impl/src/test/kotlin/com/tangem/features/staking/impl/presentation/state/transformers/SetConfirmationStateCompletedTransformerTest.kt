package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.TransactionDoneState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class SetConfirmationStateCompletedTransformerTest {

    private val confirmed = stakingUiState(
        currentStep = StakingStep.Confirmation,
        confirmationState = confirmationData(
            notifications = persistentListOf(StakingNotification.Warning.LowStakedBalance),
        ),
    )

    @Test
    fun `GIVEN data WHEN transform THEN completed with tx url and success step`() {
        // Arrange
        val before = System.currentTimeMillis()

        // Act
        val actual = transform(confirmed)

        // Assert
        val after = System.currentTimeMillis()
        val data = actual.confirmationState as StakingStates.ConfirmationState.Data
        val done = data.transactionDoneState as TransactionDoneState.Content
        assertThat(actual.currentStep).isEqualTo(StakingStep.Success)
        assertThat(data.innerState).isEqualTo(InnerConfirmationStakingState.COMPLETED)
        assertThat(data.footerText).isEqualTo(TextReference.EMPTY)
        assertThat(data.notifications).isEmpty()
        assertThat(data.isPrimaryButtonEnabled).isTrue()
        assertThat(done.txUrl).isEqualTo("https://tx")
        assertThat(done.timestamp).isIn(Range.closed(before, after))
    }

    @Test
    fun `GIVEN cached sources WHEN transform THEN primary disabled`() {
        // Act
        val actual = transform(confirmed, status = currencyStatus(networkSource = StatusSource.CACHE))

        // Assert
        assertThat((actual.confirmationState as StakingStates.ConfirmationState.Data).isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN empty confirmation WHEN transform THEN success step anyway`() {
        // Act
        val actual = transform(stakingUiState(currentStep = StakingStep.Confirmation))

        // Assert
        assertThat(actual.currentStep).isEqualTo(StakingStep.Success)
        assertThat(actual.confirmationState).isEqualTo(StakingStates.ConfirmationState.Empty())
    }

    private fun transform(state: StakingUiState, status: CryptoCurrencyStatus = currencyStatus()) =
        SetConfirmationStateCompletedTransformer(txUrl = "https://tx", cryptoCurrencyStatus = status).transform(state)
}