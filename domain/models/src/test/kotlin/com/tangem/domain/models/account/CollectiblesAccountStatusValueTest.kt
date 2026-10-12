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
        fun `GIVEN no vault read WHEN totalFiatBalance THEN the wallet total is neither stalled nor blanked`() {
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
                value = CollectiblesAccountStatusValue.MissedDerivation,
                expected = loadedZero(StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.Error.Unavailable,
                expected = loadedZero(StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.Onboarded(
                    source = StatusSource.ACTUAL,
                    vaultBalance = vaultBalance(amount = "310", rate = "1"),
                ),
                expected = TotalFiatBalance.Loaded(amount = BigDecimal("310"), source = StatusSource.ACTUAL),
            ),
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.UpdateRequired(
                    source = StatusSource.ONLY_CACHE,
                    vaultBalance = vaultBalance(amount = "310", rate = "1"),
                ),
                expected = TotalFiatBalance.Loaded(amount = BigDecimal("310"), source = StatusSource.ONLY_CACHE),
            ),
            // Something is in the vault and nothing can price it: withholding the total is the honest answer
            TotalFiatBalanceModel(
                value = CollectiblesAccountStatusValue.Onboarded(
                    source = StatusSource.ACTUAL,
                    vaultBalance = vaultBalance(amount = "2", rate = null),
                ),
                expected = TotalFiatBalance.Failed,
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
                CollectiblesAccountStatusValue.MissedDerivation,
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
            // Arrange — a vault this device cannot address yet is still a vault
            val accounts = listOf(
                CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
                CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL),
                CollectiblesAccountStatusValue.MissedDerivation,
            )

            // Act
            val actual = accounts.map { it.isAccountCreated }

            // Assert
            Truth.assertThat(actual).doesNotContain(false)
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
            CollectiblesAccountStatusValue.MissedDerivation,
            CollectiblesAccountStatusValue.Error.Unavailable,
        )
    }
}