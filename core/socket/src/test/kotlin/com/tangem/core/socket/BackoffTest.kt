package com.tangem.core.socket

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class BackoffTest {

    @Test
    fun `GIVEN no jitter WHEN attempts grow THEN delay doubles up to the cap`() {
        // Arrange
        val backoff = Backoff.exponential(min = 1.seconds, max = 30.seconds, jitter = 0.0)

        // Act
        val delays = (0..6).map(backoff::delayFor)

        // Assert
        assertThat(delays).containsExactly(1.seconds, 2.seconds, 4.seconds, 8.seconds, 16.seconds, 30.seconds, 30.seconds)
            .inOrder()
    }

    @Test
    fun `GIVEN jitter WHEN delay computed THEN it stays within the jitter band`() {
        // Arrange
        val backoff = Backoff.exponential(min = 1.seconds, max = 30.seconds, jitter = 0.2, random = Random(seed = 7))

        // Act
        val delays = List(100) { backoff.delayFor(attempt = 3) }

        // Assert
        assertThat(delays.min()).isAtLeast(6.4.seconds)
        assertThat(delays.max()).isAtMost(9.6.seconds)
    }

    @Test
    fun `GIVEN jitter at the cap WHEN delay computed THEN it never exceeds max`() {
        // Arrange
        val backoff = Backoff.exponential(min = 1.seconds, max = 30.seconds, jitter = 0.5, random = Random(seed = 7))

        // Act
        val delays = List(100) { backoff.delayFor(attempt = 10) }

        // Assert
        assertThat(delays.max()).isAtMost(30.seconds)
    }

    @Test
    fun `GIVEN invalid bounds or jitter WHEN backoff created THEN it is rejected`() {
        // Arrange
        val invalid = listOf<() -> Backoff>(
            { Backoff.exponential(min = Duration.ZERO) },
            { Backoff.exponential(min = 5.seconds, max = 1.seconds) },
            { Backoff.exponential(jitter = 1.0) },
            { Backoff.exponential(jitter = -0.1) },
        )

        // Act
        val errors = invalid.map { create -> runCatching { create() }.exceptionOrNull() }

        // Assert
        errors.forEach { error -> assertThat(error).isInstanceOf(IllegalArgumentException::class.java) }
    }
}