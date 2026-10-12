package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Verifies the contribution [PredictionAccountStatusValue] makes to the wallet total.
 *
 * The wallet total is the sum of every account's contribution, and a single [TotalFiatBalance.Loading] or
 * [TotalFiatBalance.Failed] one short-circuits it. So the states that carry no balance must contribute a loaded
 * zero, otherwise a prediction account that is still fetching, or one whose collateral this device cannot read
 * at all, hides the balance the user already had.
 */
internal class PredictionAccountStatusValueTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TotalFiatBalanceOf {

        @ParameterizedTest
        @ProvideTestModels
        fun totalFiatBalance(model: TotalFiatBalanceModel) {
            // Act
            val actual = model.value.totalFiatBalance

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN a wallet set up elsewhere WHEN totalFiatBalance THEN it never blanks the wallet total`() {
            // Arrange
            val value = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

            // Act
            val actual = value.totalFiatBalance

            // Assert
            Truth.assertThat(actual).isNotEqualTo(TotalFiatBalance.Failed)
        }

        @Test
        fun `GIVEN any state WHEN totalFiatBalance THEN it never stalls the wallet total`() {
            // Arrange
            val values = provideTestModels().map(TotalFiatBalanceModel::value)

            // Act
            val actual = values.map(PredictionAccountStatusValue::totalFiatBalance)

            // Assert
            Truth.assertThat(actual).doesNotContain(TotalFiatBalance.Loading)
        }

        private fun provideTestModels() = listOf(
            TotalFiatBalanceModel(
                value = PredictionAccountStatusValue.Loading,
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.CACHE),
            ),
            TotalFiatBalanceModel(
                value = PredictionAccountStatusValue.NotOnboarded,
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = onboarding(
                    source = StatusSource.ACTUAL,
                    stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING,
                ),
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = onboarding(
                    source = StatusSource.CACHE,
                    stage = PredictionAccountStatusValue.Onboarding.Stage.APPROVING,
                ),
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.CACHE),
            ),
            TotalFiatBalanceModel(
                value = PredictionAccountStatusValue.Error.OnboardingFailed,
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = PredictionAccountStatusValue.Error.Unavailable,
                expected = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = active(balance = BigDecimal("100"), fiatRate = BigDecimal("0.9")),
                expected = TotalFiatBalance.Loaded(
                    amount = BigDecimal("100").multiply(BigDecimal("0.9")),
                    source = StatusSource.ACTUAL,
                ),
            ),
            TotalFiatBalanceModel(
                value = active(balance = BigDecimal.ZERO, fiatRate = BigDecimal("0.9")),
                expected = TotalFiatBalance.Loaded(
                    amount = BigDecimal.ZERO.multiply(BigDecimal("0.9")),
                    source = StatusSource.ACTUAL,
                ),
            ),
            TotalFiatBalanceModel(
                value = active(balance = BigDecimal("100"), fiatRate = null),
                expected = TotalFiatBalance.Failed,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TotalBalanceOf {

        @Test
        fun `GIVEN positions value known WHEN totalBalance THEN it is the collateral plus the positions`() {
            // Arrange
            val value = active(balance = BigDecimal("10.5"), fiatRate = null, positionsValue = BigDecimal("2.25"))

            // Act
            val actual = value.totalBalance

            // Assert
            Truth.assertThat(actual).isEqualTo(BigDecimal("12.75"))
        }

        @Test
        fun `GIVEN positions value unknown WHEN totalBalance THEN it is unknown too`() {
            // Arrange
            val value = active(balance = BigDecimal("10.5"), fiatRate = null, positionsValue = null)

            // Act
            val actual = value.totalBalance

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN positions value known WHEN totalFiatBalance THEN the wallet total counts the collateral only`() {
            // Arrange
            val value = active(balance = BigDecimal("10"), fiatRate = BigDecimal("2"), positionsValue = BigDecimal("5"))

            // Act
            val actual = value.totalFiatBalance

            // Assert
            Truth.assertThat(actual).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("20"), source = StatusSource.ACTUAL),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopySealed {

        @ParameterizedTest
        @ProvideTestModels
        fun copySealed(model: CopySealedModel) {
            // Act
            val actual = model.value.copySealed(source = StatusSource.ONLY_CACHE)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CopySealedModel(
                value = PredictionAccountStatusValue.Loading,
                expected = PredictionAccountStatusValue.Loading,
            ),
            CopySealedModel(
                value = PredictionAccountStatusValue.NotOnboarded,
                expected = PredictionAccountStatusValue.NotOnboarded,
            ),
            CopySealedModel(
                value = PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
                expected = PredictionAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE),
            ),
            CopySealedModel(
                value = PredictionAccountStatusValue.Error.OnboardingFailed,
                expected = PredictionAccountStatusValue.Error.OnboardingFailed,
            ),
            CopySealedModel(
                value = PredictionAccountStatusValue.Error.Unavailable,
                expected = PredictionAccountStatusValue.Error.Unavailable,
            ),
            CopySealedModel(
                value = onboarding(
                    source = StatusSource.ACTUAL,
                    stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
                ),
                expected = onboarding(
                    source = StatusSource.ONLY_CACHE,
                    stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
                ),
            ),
            CopySealedModel(
                value = active(balance = BigDecimal("1"), fiatRate = BigDecimal("1")),
                expected = active(
                    balance = BigDecimal("1"),
                    fiatRate = BigDecimal("1"),
                    source = StatusSource.ONLY_CACHE,
                ),
            ),
        )
    }

    internal data class TotalFiatBalanceModel(
        val value: PredictionAccountStatusValue,
        val expected: TotalFiatBalance,
    )

    internal data class CopySealedModel(
        val value: PredictionAccountStatusValue,
        val expected: PredictionAccountStatusValue,
    )

    private companion object {
        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)


        fun onboarding(source: StatusSource, stage: PredictionAccountStatusValue.Onboarding.Stage) =
            PredictionAccountStatusValue.Onboarding(source = source, stage = stage)

        fun active(
            balance: BigDecimal,
            fiatRate: BigDecimal?,
            source: StatusSource = StatusSource.ACTUAL,
            positionsValue: BigDecimal? = null,
        ) = PredictionAccountStatusValue.Active(
            source = source,
            balance = balance,
            positionsValue = positionsValue,
            fiatRate = fiatRate,
            depositCurrency = COLLATERAL,
            depositAddress = "0xbridge",
        )
    }
}