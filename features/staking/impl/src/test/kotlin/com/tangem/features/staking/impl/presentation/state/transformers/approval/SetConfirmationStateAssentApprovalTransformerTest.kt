package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

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
                every { contributions } returns emptyList()
            },
        ),
        isApprovalInProgress = isApprovalInProgress,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN confirmation data WHEN approval fee set THEN assent state with approval fee`(model: ApprovalFeeModel) {
        // Arrange
        val confirmation = confirmationData(innerState = InnerConfirmationStakingState.IN_PROGRESS)
        val state = stakingUiState(confirmationState = confirmation)

        // Act
        val actual = feeTransformer(model).transform(state)

        // Assert
        assertThat(actual).isEqualTo(
            state.copy(
                confirmationState = confirmation.copy(
                    innerState = InnerConfirmationStakingState.ASSENT,
                    feeState = FeeState.Content(
                        fee = approvalFee,
                        rate = model.expectedRate,
                        isFeeConvertibleToFiat = model.expectedConvertible,
                        appCurrency = AppCurrency.Default,
                        isFeeApproximate = false,
                    ),
                    isPrimaryButtonEnabled = model.expectedPrimaryEnabled,
                    isApprovalNeeded = true,
                ),
                bottomSheetConfig = null,
            ),
        )
    }

    @Test
    fun `GIVEN no confirmation data WHEN transform THEN confirmation unchanged and sheet closed`() {
        // Arrange
        val state = stakingUiState()

        // Act
        val actual = feeTransformer(ApprovalFeeModel(feeStatus = null)).transform(state)

        // Assert
        assertThat(actual).isEqualTo(state.copy(bottomSheetConfig = null))
    }

    private fun feeTransformer(model: ApprovalFeeModel) = SetConfirmationStateAssentApprovalTransformer(
        appCurrencyProvider = Provider { AppCurrency.Default },
        feeCryptoCurrencyStatus = model.feeStatus,
        fee = TransactionFee.Single(approvalFee),
        cryptoCurrencyStatus = model.status,
        isApprovalInProgress = model.isApprovalInProgress,
    )

    data class ApprovalFeeModel(
        val feeStatus: CryptoCurrencyStatus?,
        val status: CryptoCurrencyStatus = currencyStatus(),
        val isApprovalInProgress: Boolean = false,
        val expectedRate: BigDecimal? = null,
        val expectedConvertible: Boolean = false,
        val expectedPrimaryEnabled: Boolean = true,
    )

    companion object {
        private val approvalFee = ethFee(BigDecimal("0.01"))

        @JvmStatic
        fun provideTestModels() = listOf(
            ApprovalFeeModel(feeStatus = null),
            ApprovalFeeModel(
                feeStatus = currencyStatus(fiatRate = BigDecimal("2")),
                expectedRate = BigDecimal("2"),
                expectedConvertible = true,
            ),
            ApprovalFeeModel(
                feeStatus = null,
                status = currencyStatus(networkSource = StatusSource.CACHE),
                expectedPrimaryEnabled = false,
            ),
        )
    }
}