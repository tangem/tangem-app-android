package com.tangem.domain.models.currency.balance

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BalanceContributionsTest {

    private val userWallet: UserWallet = mockk()

    @Nested
    inner class ResolversFlow {

        @Test
        fun `GIVEN providers WHEN resolversFlow THEN frames of every provider in order`() = runTest {
            // Arrange
            val alphaResolver = ContributionResolver { _, _ -> null }
            val betaResolver = ContributionResolver { _, _ -> null }
            val providers = listOf(FakeProvider(resolver = alphaResolver), FakeProvider(resolver = betaResolver))

            // Act
            val actual = providers.resolversFlow(userWallet = userWallet).first()

            // Assert
            assertThat(actual).containsExactly(alphaResolver, betaResolver).inOrder()
        }

        @Test
        fun `GIVEN no providers WHEN resolversFlow THEN an empty frame straight away`() = runTest {
            // Act
            val actual = emptyList<BalanceContributionProvider>().resolversFlow(userWallet = userWallet).first()

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    inner class WithSpendableContributions {

        @Test
        fun `GIVEN nothing spendable WHEN withSpendableContributions THEN the same amount, scale included`() {
            // Arrange
            val amount = BigDecimal("1.500")

            // Act
            val actual = amount.withSpendableContributions(listOf(Spendable(delta = BigDecimal.ZERO)))

            // Assert
            assertThat(actual).isSameInstanceAs(amount)
        }

        @Test
        fun `GIVEN spendable contributions WHEN withSpendableContributions THEN all of them are added`() {
            // Arrange — powers of two, so omitting any single one produces a distinct sum
            val contributions = listOf(Spendable(delta = BigDecimal(1)), Spendable(delta = BigDecimal(2)))

            // Act
            val actual = BigDecimal(4).withSpendableContributions(contributions)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(7))
        }
    }

    private class FakeProvider(private val resolver: ContributionResolver) : BalanceContributionProvider {

        override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> = flowOf(resolver)
    }

    private data class Spendable(val delta: BigDecimal) : BalanceContribution {
        override val source: StatusSource = StatusSource.ACTUAL
        override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal.ZERO
        override fun spendableDeltaCryptoAmount(): BigDecimal = delta
    }
}