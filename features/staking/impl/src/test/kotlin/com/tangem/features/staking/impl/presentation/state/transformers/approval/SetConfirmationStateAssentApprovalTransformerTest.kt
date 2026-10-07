package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class SetConfirmationStateAssentApprovalTransformerTest {

    @Test
    fun `GIVEN approval in progress WHEN transform THEN primary button disabled`() {
        // Arrange
        val transformer = createTransformer(isApprovalInProgress = true)

        // Act
        val actual = transformer.transform(createAssentConfirmationUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN no approval in progress AND sources actual WHEN transform THEN primary button enabled`() {
        // Arrange
        val transformer = createTransformer(isApprovalInProgress = false)

        // Act
        val actual = transformer.transform(createAssentConfirmationUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isTrue()
    }

    private fun createTransformer(isApprovalInProgress: Boolean) = SetConfirmationStateAssentApprovalTransformer(
        appCurrencyProvider = Provider { AppCurrency.Default },
        feeCryptoCurrencyStatus = null,
        fee = mockk(relaxed = true),
        cryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = mockk(relaxed = true),
            value = mockk(relaxed = true) {
                every { sources } returns CryptoCurrencyStatus.Sources()
                every { stakingBalance } returns null
                every { contributions } returns emptyList()
            },
        ),
        isApprovalInProgress = isApprovalInProgress,
    )
}