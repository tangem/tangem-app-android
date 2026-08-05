package com.tangem.domain.models.currency.balance

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests the generic half of the extra-balance contract — the two functions every balance type passes through,
 * whichever type it is.
 *
 * Both are deliberately type-blind, so the cases worth pinning are the ones a single-contribution fixture cannot
 * reach: summing more than one contribution, selecting between competing types, and telling "no extra balance"
 * (`null`) apart from "an extra balance that happens to be worth zero" (`ZERO`). That last distinction is
 * load-bearing — callers return the bare, possibly-`null`, amount for `null`, so collapsing the two would turn a
 * missing amount into `0`.
 *
 * Contributions here are local [Alpha] / [Beta] types rather than real balances: these functions must not know any
 * concrete type, and a test that reached for `StakingBalance` would quietly stop proving that.
 */
internal class BalanceContributionExtTest {

    @Nested
    inner class TotalContributionsDeltaOrNull {

        @Test
        fun `GIVEN no contributions WHEN totalContributionsDeltaOrNull THEN null`() {
            // Arrange
            val value = value(contributions = emptyList())

            // Act
            val actual = value.totalContributionsDeltaOrNull()

            // Assert — not zero: the currency has nothing beyond its network amount
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN several contributions WHEN totalContributionsDeltaOrNull THEN all of them are summed`() {
            // Arrange — powers of two, so omitting any single one produces a distinct total
            val value = value(contributions = listOf(Alpha(delta = 1), Beta(delta = 2), Alpha(delta = 4)))

            // Act
            val actual = value.totalContributionsDeltaOrNull()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(7))
        }

        @Test
        fun `GIVEN a single zero contribution WHEN totalContributionsDeltaOrNull THEN zero and not null`() {
            // Arrange — the shape of a real yield-supply contribution: present, but numerically inert
            val value = value(contributions = listOf(Alpha(delta = 0)))

            // Act
            val actual = value.totalContributionsDeltaOrNull()

            // Assert — "has a contribution worth zero" must not collapse into "has no contribution"
            assertThat(actual).isEqualTo(BigDecimal.ZERO)
        }

        @Test
        fun `GIVEN an inert contribution beside a real one WHEN totalContributionsDeltaOrNull THEN only the real counts`() {
            // Arrange — staking plus yield supply, the combination the seam actually produces
            val value = value(contributions = listOf(Beta(delta = 0), Alpha(delta = 5)))

            // Act
            val actual = value.totalContributionsDeltaOrNull()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(5))
        }
    }

    @Nested
    inner class ContributionOrNull {

        @Test
        fun `GIVEN no contributions WHEN contributionOrNull THEN null`() {
            // Arrange
            val value = value(contributions = emptyList())

            // Act
            val actual = value.contributionOrNull<Alpha>()

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN only another type WHEN contributionOrNull THEN null`() {
            // Arrange
            val value = value(contributions = listOf(Beta(delta = 1)))

            // Act
            val actual = value.contributionOrNull<Alpha>()

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN another type first WHEN contributionOrNull THEN the matching one is still found`() {
            // Arrange
            val alpha = Alpha(id = "wanted")
            val value = value(contributions = listOf(Beta(delta = 1), alpha))

            // Act — selection is by type, not by position
            val actual = value.contributionOrNull<Alpha>()

            // Assert
            assertThat(actual).isEqualTo(alpha)
        }

        @Test
        fun `GIVEN two contributions of the type WHEN contributionOrNull THEN the first one`() {
            // Arrange
            val first = Alpha(id = "first")
            val second = Alpha(id = "second")
            val value = value(contributions = listOf(first, second))

            // Act
            val actual = value.contributionOrNull<Alpha>()

            // Assert
            assertThat(actual).isEqualTo(first)
        }
    }

    private companion object {

        /**
         * [CryptoCurrencyStatus.NoQuote] is the lightest `Value` that carries contributions — `Value` is sealed, so
         * an anonymous stand-in is not an option, and the other shapes only add fields neither function reads.
         */
        fun value(contributions: List<BalanceContribution>): CryptoCurrencyStatus.Value {
            return CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.TEN,
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = mockk(relaxed = true),
                sources = CryptoCurrencyStatus.Sources(),
                contributions = contributions,
            )
        }
    }
}

/** Two unrelated implementations, so [contributionOrNull] has something to select *between*. */
private data class Alpha(val id: String = "alpha", val delta: Int = 0) : BalanceContribution {

    override val kind: String = "alpha"
    override val source: StatusSource = StatusSource.ACTUAL
    override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal(delta)
}

private data class Beta(val id: String = "beta", val delta: Int = 0) : BalanceContribution {

    override val kind: String = "beta"
    override val source: StatusSource = StatusSource.ACTUAL
    override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal(delta)
}