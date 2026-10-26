package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.utils.Provider
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetConfirmationStateAssentTransformerTest {

    private val fee = ethFee(BigDecimal("0.01"))

    @Test
    fun `GIVEN data and fee status WHEN transform THEN content fee and primary by sources`() {
        // Arrange
        val feeStatus = currencyStatus(fiatRate = BigDecimal("3"))

        // Act
        val actual = confirmation(transform(feeStatus = feeStatus))

        // Assert
        assertThat(actual.feeState).isEqualTo(
            FeeState.Content(
                fee = fee,
                rate = BigDecimal("3"),
                isFeeConvertibleToFiat = feeStatus.currency.network.hasFiatFeeRate,
                appCurrency = AppCurrency.Default,
                isFeeApproximate = false,
            ),
        )
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN cached sources WHEN transform THEN primary disabled`() {
        // Act
        val actual = confirmation(transform(status = currencyStatus(networkSource = StatusSource.CACHE)))

        // Assert
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN null fee status WHEN transform THEN null rate not convertible`() {
        // Act
        val feeState = confirmation(transform(feeStatus = null)).feeState as FeeState.Content

        // Assert
        assertThat(feeState.rate).isNull()
        assertThat(feeState.isFeeConvertibleToFiat).isFalse()
    }

    @Test
    fun `GIVEN empty confirmation WHEN transform THEN unchanged`() {
        // Act
        val actual = transform(state = stakingUiState())

        // Assert
        assertThat(actual.confirmationState).isEqualTo(StakingStates.ConfirmationState.Empty())
    }

    private fun transform(
        feeStatus: CryptoCurrencyStatus? = currencyStatus(),
        status: CryptoCurrencyStatus = currencyStatus(),
        state: StakingUiState = stakingUiState(confirmationState = confirmationData()),
    ) = SetConfirmationStateAssentTransformer(
        appCurrencyProvider = Provider { AppCurrency.Default },
        feeCryptoCurrencyStatus = feeStatus,
        fee = fee,
        isFeeApproximate = false,
        cryptoCurrencyStatus = status,
    ).transform(state)

    private fun confirmation(state: StakingUiState) = state.confirmationState as StakingStates.ConfirmationState.Data
}