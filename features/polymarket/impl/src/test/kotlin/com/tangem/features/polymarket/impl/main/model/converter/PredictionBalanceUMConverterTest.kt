package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.features.polymarket.impl.main.ui.state.PredictionBalanceUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionBalanceUMConverterTest {

    private val collateral: CryptoCurrency = mockk {
        every { symbol } returns "USDC"
        every { decimals } returns 6
    }

    private val converter = PredictionBalanceUMConverter(
        appCurrency = AppCurrency.Default,
        collateral = collateral,
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
    fun `GIVEN a funded account WHEN converted THEN both figures are offered`() {
        // Arrange
        val status = active(balance = "12.5", isTradingAllowed = true)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.amount).isInstanceOf(PredictionBalanceUM.AmountUM.Known::class.java)
    }

    @Test
    fun `GIVEN a cached figure WHEN converted THEN the amount is marked as flickering`() {
        // Arrange
        val status = PredictionAccountStatusValue.Active(
            source = StatusSource.CACHE,
            balance = BigDecimal("12.5"),
            fiatRate = BigDecimal.ONE,
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat((actual.amount as PredictionBalanceUM.AmountUM.Known).isFlickering).isTrue()
    }

    @Test
    fun `GIVEN a balance that cannot be priced WHEN converted THEN the amount is unknown`() {
        // Arrange
        val status = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("12.5"),
            fiatRate = null,
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.amount).isEqualTo(PredictionBalanceUM.AmountUM.Unknown)
    }

    @Test
    fun `GIVEN a wallet onboarded elsewhere WHEN converted THEN the amount is unknown rather than zero`() {
        // Arrange
        val status = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.amount).isEqualTo(PredictionBalanceUM.AmountUM.Unknown)
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