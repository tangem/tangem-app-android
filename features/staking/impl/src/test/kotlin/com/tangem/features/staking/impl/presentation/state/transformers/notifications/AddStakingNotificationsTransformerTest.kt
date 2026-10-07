package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class AddStakingNotificationsTransformerTest {

    @Test
    fun `GIVEN approval in progress WHEN transform THEN approval notification kept AND primary button disabled`() {
        // Arrange
        val transformer = createTransformer(isApprovalInProgress = true)

        // Act
        val actual = transformer.transform(createAssentConfirmationUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.notifications).contains(StakingNotification.Warning.TransactionInProgress.Approval)
        assertThat(confirmation.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN no approval in progress WHEN transform THEN primary button enabled`() {
        // Arrange
        val transformer = createTransformer(isApprovalInProgress = false)

        // Act
        val actual = transformer.transform(createAssentConfirmationUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.notifications)
            .doesNotContain(StakingNotification.Warning.TransactionInProgress.Approval)
        assertThat(confirmation.isPrimaryButtonEnabled).isTrue()
    }

    // region Fixtures
    private fun createTransformer(isApprovalInProgress: Boolean): AddStakingNotificationsTransformer {
        val status = CryptoCurrencyStatus(
            currency = mockk(relaxed = true),
            value = mockk(relaxed = true) {
                every { sources } returns CryptoCurrencyStatus.Sources()
                every { stakingBalance } returns null
                every { contributions } returns emptyList()
            },
        )
        return AddStakingNotificationsTransformer(
            cryptoCurrencyStatusProvider = Provider { status },
            appCurrencyProvider = Provider { AppCurrency.Default },
            isAccountInitializedProvider = Provider { true },
            feeCryptoCurrencyStatus = null,
            currencyWarning = null,
            feeError = null,
            stakingError = null,
            currencyCheck = CryptoCurrencyCheck(
                dustValue = null,
                reserveAmount = null,
                minimumSendAmount = null,
                existentialDeposit = null,
                utxoAmountLimit = null,
                isAccountFunded = true,
                rentWarning = null,
            ),
            isSubtractAvailable = false,
            isApprovalInProgress = isApprovalInProgress,
            integration = mockk(relaxed = true),
        )
    }
    // endregion
}