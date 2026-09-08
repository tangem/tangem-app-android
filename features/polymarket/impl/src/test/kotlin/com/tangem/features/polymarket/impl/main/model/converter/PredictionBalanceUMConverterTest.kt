package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionBalanceUMConverterTest {

    private val converter = PredictionBalanceUMConverter(
        onAddFundsClick = {},
        onWithdrawClick = {},
    )

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: GatingModel) {
        // Arrange
        val status = model.status

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.addFunds.isEnabled).isEqualTo(model.addFundsEnabled)
        assertThat(actual.withdraw.isEnabled).isEqualTo(model.withdrawEnabled)
        assertThat(actual.addFunds.isLoading).isEqualTo(model.isLoading)
        assertThat(actual.withdraw.isLoading).isEqualTo(model.isLoading)
    }

    @Test
    fun `GIVEN a funded account WHEN converted THEN the balance is stated`() {
        // Arrange
        val status = active(balance = "12.5", isTradingAllowed = true)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.balance).isNotEqualTo(stringReference(value = EMPTY_BALANCE_SIGN))
    }

    @Test
    fun `GIVEN a wallet onboarded elsewhere WHEN converted THEN the balance reads as unknown`() {
        // Arrange
        val status = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.balance).isEqualTo(stringReference(value = EMPTY_BALANCE_SIGN))
    }

    @Test
    fun `GIVEN the account is still loading WHEN converted THEN the balance reads as unknown`() {
        // Arrange
        val status = PredictionAccountStatusValue.Loading

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.balance).isEqualTo(stringReference(value = EMPTY_BALANCE_SIGN))
    }

    internal data class GatingModel(
        val status: PredictionAccountStatusValue,
        val addFundsEnabled: Boolean,
        val withdrawEnabled: Boolean,
        val isLoading: Boolean = false,
    )

    private fun active(balance: String, isTradingAllowed: Boolean) = PredictionAccountStatusValue.Active(
        source = StatusSource.ACTUAL,
        balance = BigDecimal(balance),
        fiatRate = BigDecimal.ONE,
        isTradingAllowed = isTradingAllowed,
        depositCurrency = mockk(relaxed = true),
        depositAddress = "0xbridge",
    )

    private fun provideTestModels() = listOf(
        GatingModel(
            status = PredictionAccountStatusValue.Loading,
            addFundsEnabled = false,
            withdrawEnabled = false,
            isLoading = true,
        ),
        GatingModel(
            status = active(balance = "12.5", isTradingAllowed = true),
            addFundsEnabled = true,
            withdrawEnabled = true,
        ),
        GatingModel(
            status = active(balance = "0", isTradingAllowed = true),
            addFundsEnabled = true,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = active(balance = "12.5", isTradingAllowed = false),
            addFundsEnabled = false,
            withdrawEnabled = true,
        ),
        GatingModel(
            status = active(balance = "0", isTradingAllowed = false),
            addFundsEnabled = false,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
            addFundsEnabled = false,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = PredictionAccountStatusValue.NotOnboarded,
            addFundsEnabled = false,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = PredictionAccountStatusValue.Onboarding(
                source = StatusSource.ACTUAL,
                stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING,
            ),
            addFundsEnabled = false,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = PredictionAccountStatusValue.Error.Unavailable,
            addFundsEnabled = false,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = PredictionAccountStatusValue.Error.OnboardingFailed,
            addFundsEnabled = false,
            withdrawEnabled = false,
        ),
    )
}