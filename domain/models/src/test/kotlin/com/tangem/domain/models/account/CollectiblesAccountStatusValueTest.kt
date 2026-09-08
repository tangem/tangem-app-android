package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * The wallet total is the sum of every account's contribution, and a single [TotalFiatBalance.Loading] or
 * [TotalFiatBalance.Failed] one short-circuits it. A collectibles account holds no fiat in this phase, so every
 * state has to contribute a loaded zero — otherwise it hides the balance the user already had.
 */
internal class CollectiblesAccountStatusValueTest {

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
        fun `GIVEN any state WHEN totalFiatBalance THEN the wallet total is neither stalled nor blanked`() {
            // Arrange
            val values = allStates()

            // Act
            val actual = values.map(CollectiblesAccountStatusValue::totalFiatBalance)

            // Assert
            Truth.assertThat(actual).doesNotContain(TotalFiatBalance.Loading)
            Truth.assertThat(actual).doesNotContain(TotalFiatBalance.Failed)
        }

        private fun provideTestModels() = listOf(
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.Loading,
                expected = loadedZero(StatusSource.CACHE),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.NotOnboarded,
                expected = loadedZero(StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE),
                expected = loadedZero(StatusSource.CACHE),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ONLY_CACHE),
                expected = loadedZero(StatusSource.ONLY_CACHE),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.Error.Unavailable,
                expected = loadedZero(StatusSource.ACTUAL),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopySealed {

        @Test
        fun `GIVEN a state carrying a source WHEN copySealed THEN the source is replaced`() {
            // Arrange
            val onboarded = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)
            val updateRequired = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)

            // Act & Assert
            Truth.assertThat(onboarded.copySealed(source = StatusSource.CACHE))
                .isEqualTo(CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE))
            Truth.assertThat(updateRequired.copySealed(source = StatusSource.CACHE))
                .isEqualTo(CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.CACHE))
        }

        @Test
        fun `GIVEN a state with a fixed source WHEN copySealed THEN it is returned as it is`() {
            // Arrange
            val fixed = listOf(
                CollectiblesAccountStatusValue.Loading,
                CollectiblesAccountStatusValue.NotOnboarded,
                CollectiblesAccountStatusValue.Error.Unavailable,
            )

            // Act
            val actual = fixed.map { it.copySealed(source = StatusSource.ONLY_CACHE) }

            // Assert
            Truth.assertThat(actual).isEqualTo(fixed)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsAccountCreated {

        @Test
        fun `GIVEN a vault on chain WHEN isAccountCreated THEN true whichever device it answers to`() {
            // Arrange
            val onboarded = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)
            val updateRequired = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)

            // Act & Assert
            Truth.assertThat(onboarded.isAccountCreated).isTrue()
            Truth.assertThat(updateRequired.isAccountCreated).isTrue()
        }

        @Test
        fun `GIVEN no vault or no record WHEN isAccountCreated THEN false`() {
            // Arrange
            val notAnAccount = listOf(
                null,
                CollectiblesAccountStatusValue.Loading,
                CollectiblesAccountStatusValue.NotOnboarded,
                CollectiblesAccountStatusValue.Error.Unavailable,
            )

            // Act
            val actual = notAnAccount.map { it.isAccountCreated }

            // Assert
            Truth.assertThat(actual).doesNotContain(true)
        }
    }

    internal data class TotalFiatBalanceModel(
        val value: CollectiblesAccountStatusValue,
        val expected: TotalFiatBalance,
    )

    private companion object {

        fun loadedZero(source: StatusSource) = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = source)

        fun allStates() = listOf(
            CollectiblesAccountStatusValue.Loading,
            CollectiblesAccountStatusValue.NotOnboarded,
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
            CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL),
            CollectiblesAccountStatusValue.Error.Unavailable,
        )
    }
}