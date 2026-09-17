package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.main.model.RequisitesState
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
        val actual = converter.convert(
            PredictionBalanceUMConverter.Input(
                status = status,
                requisites = RequisitesState.Ready(target = DEPOSIT_TARGET),
                isTradingAllowed = model.isTradingAllowed,
            ),
        )

        // Assert
        assertThat(actual.addFunds.isEnabled).isEqualTo(model.addFundsEnabled)
        assertThat(actual.withdraw.isEnabled).isEqualTo(model.withdrawEnabled)
        assertThat(actual.addFunds.isLoading).isEqualTo(model.isLoading)
        assertThat(actual.withdraw.isLoading).isEqualTo(model.isLoading)
    }

    @Test
    fun `GIVEN a zero balance WHEN converting THEN withdraw is disabled`() {
        // Arrange
        val status = active(balance = "0")

        // Act
        val actual = converter.convert(
            PredictionBalanceUMConverter.Input(
                status = status,
                requisites = RequisitesState.Ready(target = DEPOSIT_TARGET),
                isTradingAllowed = true,
            ),
        )

        // Assert
        assertThat(actual.withdraw.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN a positive balance and failed deposit requisites WHEN converting THEN withdraw stays enabled`() {
        // Arrange
        val status = active(balance = "12.5")

        // Act
        val actual = converter.convert(
            PredictionBalanceUMConverter.Input(
                status = status,
                requisites = RequisitesState.Failed,
                isTradingAllowed = true,
            ),
        )

        // Assert
        assertThat(actual.withdraw.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN a funded account WHEN converted THEN the balance is stated`() {
        // Arrange
        val status = active(balance = "12.5")

        // Act
        val actual = converter.convert(
            PredictionBalanceUMConverter.Input(status = status, requisites = RequisitesState.Loading, isTradingAllowed = true),
        )

        // Assert
        assertThat(actual.balance).isNotEqualTo(stringReference(value = EMPTY_BALANCE_SIGN))
    }

    @Test
    fun `GIVEN a wallet onboarded elsewhere WHEN converted THEN the balance reads as unknown`() {
        // Arrange
        val status = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

        // Act
        val actual = converter.convert(
            PredictionBalanceUMConverter.Input(status = status, requisites = RequisitesState.Loading, isTradingAllowed = true),
        )

        // Assert
        assertThat(actual.balance).isEqualTo(stringReference(value = EMPTY_BALANCE_SIGN))
    }

    @Test
    fun `GIVEN the account is still loading WHEN converted THEN the balance reads as unknown`() {
        // Arrange
        val status = PredictionAccountStatusValue.Loading

        // Act
        val actual = converter.convert(
            PredictionBalanceUMConverter.Input(status = status, requisites = RequisitesState.Loading, isTradingAllowed = true),
        )

        // Assert
        assertThat(actual.balance).isEqualTo(stringReference(value = EMPTY_BALANCE_SIGN))
    }

    @Test
    fun `GIVEN requisites still loading WHEN converted THEN add funds is disabled and not loading`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE,
            requisites = RequisitesState.Loading,
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isFalse()
        assertThat(actual.addFunds.isDimmed).isFalse()
        assertThat(actual.addFunds.isLoading).isFalse()
    }

    @Test
    fun `GIVEN requisites failed WHEN converted THEN add funds is dimmed but still tappable`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE,
            requisites = RequisitesState.Failed,
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isTrue()
        assertThat(actual.addFunds.isDimmed).isTrue()
    }

    @Test
    fun `GIVEN requisites ready WHEN converted THEN add funds is not dimmed`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE,
            requisites = RequisitesState.Ready(target = DEPOSIT_TARGET),
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isDimmed).isFalse()
    }

    @Test
    fun `GIVEN requisites ready WHEN converted THEN add funds is enabled`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE,
            requisites = RequisitesState.Ready(target = DEPOSIT_TARGET),
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN trading not allowed WHEN requisites are ready THEN add funds stays disabled`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE,
            requisites = RequisitesState.Ready(target = DEPOSIT_TARGET),
            isTradingAllowed = false,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN no deposit address WHEN requisites are ready THEN add funds stays disabled`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE.copy(depositAddress = null),
            requisites = RequisitesState.Ready(target = DEPOSIT_TARGET),
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN no deposit address WHEN requisites failed THEN add funds is dimmed but still tappable`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = ACTIVE.copy(depositAddress = null),
            requisites = RequisitesState.Failed,
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isTrue()
        assertThat(actual.addFunds.isDimmed).isTrue()
    }

    @Test
    fun `GIVEN the account is not active WHEN requisites failed THEN add funds stays disabled`() {
        // Arrange
        val input = PredictionBalanceUMConverter.Input(
            status = PredictionAccountStatusValue.NotOnboarded,
            requisites = RequisitesState.Failed,
            isTradingAllowed = true,
        )

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.addFunds.isEnabled).isFalse()
    }

    internal data class GatingModel(
        val status: PredictionAccountStatusValue,
        val addFundsEnabled: Boolean,
        val withdrawEnabled: Boolean,
        val isLoading: Boolean = false,
        val isTradingAllowed: Boolean = true,
    )

    private fun provideTestModels() = listOf(
        GatingModel(
            status = PredictionAccountStatusValue.Loading,
            addFundsEnabled = false,
            withdrawEnabled = false,
            isLoading = true,
        ),
        GatingModel(
            status = active(balance = "12.5"),
            addFundsEnabled = true,
            withdrawEnabled = true,
        ),
        GatingModel(
            status = active(balance = "0"),
            addFundsEnabled = true,
            withdrawEnabled = false,
        ),
        GatingModel(
            status = active(balance = "12.5"),
            isTradingAllowed = false,
            addFundsEnabled = false,
            withdrawEnabled = true,
        ),
        GatingModel(
            status = active(balance = "0"),
            isTradingAllowed = false,
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

    private companion object {

        fun active(balance: String) = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal(balance),
            fiatRate = BigDecimal.ONE,
            depositCurrency = mockk(relaxed = true),
            depositAddress = "0xbridge",
        )

        val ACTIVE = active(balance = "12.5")

        val DEPOSIT_TARGET = PolymarketDepositTarget(
            network = PolymarketDepositNetwork(
                chainId = PolymarketContracts.CHAIN_ID.toString(),
                chainName = "Polygon",
                family = PolymarketChainFamily.EVM,
                address = "0xrequisites",
                tokens = emptyList(),
            ),
            token = PolymarketDepositToken(
                symbol = "USDC",
                address = PolymarketContracts.USDC_TOKEN_ADDRESS,
                decimals = 6,
                minCheckoutUsd = BigDecimal("2"),
            ),
        )
    }
}