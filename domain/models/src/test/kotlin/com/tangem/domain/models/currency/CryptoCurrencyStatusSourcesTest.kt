package com.tangem.domain.models.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import org.junit.jupiter.api.Test

/**
 * Tests [CryptoCurrencyStatus.Sources.total] — the aggregate freshness of a status, and the shared
 * `getResultStatusSource()` fold behind it. Neither had a test before.
 *
 * `total` answers "is anything behind this number stale?", which is what a shimmer, a sync icon or a
 * "using outdated data" banner needs beside a total that includes the extra balances. Note what that means for
 * [CryptoCurrencyStatus.Sources.contributionSources]: extra-balance staleness reaches `total` generically, so a
 * new contribution type degrades the indicator without anyone editing this class.
 */
internal class CryptoCurrencyStatusSourcesTest {

    @Test
    fun `GIVEN every dimension actual WHEN total THEN actual`() {
        // Act
        val actual = CryptoCurrencyStatus.Sources().total

        // Assert
        assertThat(actual).isEqualTo(StatusSource.ACTUAL)
    }

    @Test
    fun `GIVEN a cached network WHEN total THEN cache`() {
        // Arrange
        val sources = CryptoCurrencyStatus.Sources(networkSource = StatusSource.CACHE)

        // Act & Assert
        assertThat(sources.total).isEqualTo(StatusSource.CACHE)
    }

    @Test
    fun `GIVEN a cached quote WHEN total THEN cache`() {
        // Arrange
        val sources = CryptoCurrencyStatus.Sources(quoteSource = StatusSource.CACHE)

        // Act & Assert
        assertThat(sources.total).isEqualTo(StatusSource.CACHE)
    }

    @Test
    fun `GIVEN a cached contribution WHEN total THEN cache`() {
        // Arrange — extra-balance staleness reaches the indicator through the generic list
        val sources = CryptoCurrencyStatus.Sources(contributionSources = listOf(StatusSource.CACHE))

        // Act & Assert
        assertThat(sources.total).isEqualTo(StatusSource.CACHE)
    }

    @Test
    fun `GIVEN a cached legacy staking field WHEN total THEN cache`() {
        // Arrange — the toggle-off shape, where staking staleness is still on the typed field
        val sources = CryptoCurrencyStatus.Sources(stakingBalanceSource = StatusSource.CACHE)

        // Act & Assert
        assertThat(sources.total).isEqualTo(StatusSource.CACHE)
    }

    @Test
    fun `GIVEN both cached and only-cache dimensions WHEN total THEN only cache wins`() {
        // Arrange
        val sources = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.CACHE,
            quoteSource = StatusSource.ONLY_CACHE,
            contributionSources = listOf(StatusSource.ACTUAL),
        )

        // Act & Assert — ONLY_CACHE outranks CACHE, which outranks ACTUAL
        assertThat(sources.total).isEqualTo(StatusSource.ONLY_CACHE)
    }
}