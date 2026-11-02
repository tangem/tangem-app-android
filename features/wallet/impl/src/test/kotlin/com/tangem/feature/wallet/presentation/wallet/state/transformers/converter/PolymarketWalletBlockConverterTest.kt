package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.account.PredictionPositionsValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketWalletBlockConverterTest {

    private val clickIntents: WalletClickIntents = mockk(relaxed = true)

    private val converter = PolymarketWalletBlockConverter(
        appCurrency = AppCurrency.Default,
        clickIntents = clickIntents,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: BalanceModel) {
        // Act
        val actual = converter.convert(value = model.status.toAccountStatus()) as PolymarketWalletBlockUM.Content

        // Assert
        assertThat(actual.balance).isInstanceOf(model.expected)
    }

    @Test
    fun `GIVEN a loading status WHEN convert THEN the balance shimmers instead of flickering a dash`() {
        // Act
        val actual = converter.convert(
            value = PredictionAccountStatusValue.Loading.toAccountStatus(),
        ) as PolymarketWalletBlockUM.Content

        // Assert
        assertThat(actual.balance).isEqualTo(PolymarketWalletBlockUM.Balance.Loading)
    }

    @Test
    fun `GIVEN a cached balance WHEN convert THEN the balance is marked as refreshing`() {
        // Arrange
        val status = active(source = StatusSource.CACHE)

        // Act
        val actual = converter.convert(value = status.toAccountStatus()) as PolymarketWalletBlockUM.Content

        // Assert
        assertThat(actual.isBalanceFlickering).isTrue()
        assertThat(actual.isBalanceFromCache).isFalse()
    }

    @Test
    fun `GIVEN a balance that could not be refreshed WHEN convert THEN it is marked as stale`() {
        // Arrange
        val status = active(source = StatusSource.ONLY_CACHE)

        // Act
        val actual = converter.convert(value = status.toAccountStatus()) as PolymarketWalletBlockUM.Content

        // Assert
        assertThat(actual.isBalanceFromCache).isTrue()
        assertThat(actual.isBalanceFlickering).isFalse()
    }

    @Test
    fun `GIVEN open positions WHEN convert THEN the row shows the collateral plus the positions`() {
        // Arrange
        val status = active(balance = BigDecimal("10"), positions = positions(amount = BigDecimal("5")))
        val expected = converter.convert(value = active(balance = BigDecimal("15")).toAccountStatus())

        // Act
        val actual = converter.convert(value = status.toAccountStatus())

        // Assert
        assertThat(actual).isInstanceOf(PolymarketWalletBlockUM.Content::class.java)
        assertThat((actual as PolymarketWalletBlockUM.Content).balance)
            .isEqualTo((expected as PolymarketWalletBlockUM.Content).balance)
    }

    @Test
    fun `GIVEN a positions value that could not be refreshed WHEN convert THEN the row is marked as stale`() {
        // Arrange
        val status = active(positions = positions(amount = BigDecimal.ONE, source = StatusSource.ONLY_CACHE))

        // Act
        val actual = converter.convert(value = status.toAccountStatus()) as PolymarketWalletBlockUM.Content

        // Assert
        assertThat(actual.isBalanceFromCache).isTrue()
        assertThat(actual.isBalanceFlickering).isFalse()
    }

    @Test
    fun `GIVEN a positions value never read WHEN convert THEN the collateral is shown as refreshing`() {
        // Arrange
        val status = active(positions = PredictionPositionsValue.Unknown)

        // Act
        val actual = converter.convert(value = status.toAccountStatus()) as PolymarketWalletBlockUM.Content

        // Assert
        assertThat(actual.balance).isInstanceOf(amount)
        assertThat(actual.isBalanceFlickering).isTrue()
    }

    @Test
    fun `GIVEN a converted row WHEN clicked THEN the wallet of that account is opened`() {
        // Arrange
        val actual = converter.convert(value = active().toAccountStatus()) as PolymarketWalletBlockUM.Content

        // Act
        actual.onClick()

        // Assert
        verify(exactly = 1) { clickIntents.onPredictionAccountClick(userWalletId) }
    }

    private fun PredictionAccountStatusValue.toAccountStatus() = AccountStatus.Prediction(
        account = Account.Prediction(userWalletId),
        value = this,
    )

    private fun active(
        source: StatusSource = StatusSource.ACTUAL,
        fiatRate: BigDecimal? = BigDecimal.ONE,
        balance: BigDecimal = BigDecimal.TEN,
        positions: PredictionPositionsValue = positions(amount = BigDecimal.ZERO),
    ) = PredictionAccountStatusValue.Active(
        source = source,
        balance = balance,
        fiatRate = fiatRate,
        depositCurrency = mockk(relaxed = true),
        depositAddress = null,
        positions = positions,
    )

    private fun positions(amount: BigDecimal, source: StatusSource = StatusSource.ACTUAL) =
        PredictionPositionsValue.Known(amount = amount, source = source)

    internal data class BalanceModel(val status: PredictionAccountStatusValue, val expected: Class<*>)

    private fun provideTestModels() = listOf(
        // loading shimmers like the wallet's own account rows; everything else unknown is a dash
        BalanceModel(status = PredictionAccountStatusValue.Loading, expected = loading),
        BalanceModel(status = PredictionAccountStatusValue.NotOnboarded, expected = unknown),
        BalanceModel(
            status = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
            expected = unknown,
        ),
        BalanceModel(
            status = PredictionAccountStatusValue.Onboarding(
                source = StatusSource.ACTUAL,
                stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
            ),
            expected = unknown,
        ),
        BalanceModel(status = PredictionAccountStatusValue.Error.Unavailable, expected = unknown),
        BalanceModel(status = PredictionAccountStatusValue.Error.OnboardingFailed, expected = unknown),
        // the collateral is known but nothing can price it, so there is still no amount to show
        BalanceModel(status = active(fiatRate = null), expected = unknown),
        BalanceModel(status = active(), expected = amount),
    )

    private companion object {
        val userWalletId = UserWalletId("011")
        val loading = PolymarketWalletBlockUM.Balance.Loading::class.java
        val unknown = PolymarketWalletBlockUM.Balance.Unknown::class.java
        val amount = PolymarketWalletBlockUM.Balance.Amount::class.java
    }
}