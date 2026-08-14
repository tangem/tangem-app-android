package com.tangem.domain.tokens.operations

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.BalanceContribution
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The migration gate `CryptoCurrencyStatusFactory` receives: [BalanceContributionsInput.isEnabled] decides whether
 * extra balances come from [BalanceContributionsInput.contributions] or from the legacy typed fields of
 * `CryptoCurrencyStatus.Value`.
 *
 * The two states are mutually exclusive, and an empty contribution list must NOT be mistaken for "toggle off" —
 * a currency simply may have no extra balance. Both constructors are pinned here so the gate cannot flip silently.
 */
internal class BalanceContributionsInputTest {

    @Test
    fun `GIVEN Disabled WHEN read THEN the legacy path is selected and nothing is contributed`() {
        // Act
        val actual = BalanceContributionsInput.Disabled

        // Assert
        assertThat(actual).isEqualTo(BalanceContributionsInput(contributions = emptyList(), isEnabled = false))
    }

    @Test
    fun `GIVEN contributions WHEN enabled THEN they are carried over unchanged and the path is on`() {
        // Arrange
        val contributions = listOf(contribution(delta = 1), contribution(delta = 2))

        // Act
        val actual = BalanceContributionsInput.enabled(contributions)

        // Assert
        assertThat(actual).isEqualTo(BalanceContributionsInput(contributions = contributions, isEnabled = true))
    }

    @Test
    fun `GIVEN no contributions WHEN enabled THEN the path is still on`() {
        // Act — a currency with no extra balance must stay on the contributions path, not fall back to the legacy one
        val actual = BalanceContributionsInput.enabled(contributions = emptyList())

        // Assert
        assertThat(actual).isEqualTo(BalanceContributionsInput(contributions = emptyList(), isEnabled = true))
    }

    /** Opaque payload: the input only transports contributions, it never inspects or sums them. */
    private fun contribution(delta: Int) = object : BalanceContribution {
        override val kind: String = "test"
        override val source: StatusSource = StatusSource.ACTUAL
        override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal(delta)
    }
}