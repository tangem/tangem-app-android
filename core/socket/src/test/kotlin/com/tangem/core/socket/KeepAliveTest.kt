package com.tangem.core.socket

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class KeepAliveTest {

    @Test
    fun `GIVEN non-positive interval WHEN client keep-alive created THEN it is rejected`() {
        // Arrange
        val intervals = listOf(Duration.ZERO, (-1).seconds)

        // Act
        val errors = intervals.map { interval ->
            runCatching { KeepAlive.ClientSends(text = "PING", interval = interval) }.exceptionOrNull()
        }

        // Assert
        assertThat(errors).hasSize(2)
        errors.forEach { error -> assertThat(error).isInstanceOf(IllegalArgumentException::class.java) }
    }
}