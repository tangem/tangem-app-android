package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PredictionClaimStateTest {

    @Test
    fun `GIVEN a known raw state WHEN parsed THEN it maps case-insensitively`() {
        // Arrange
        val raw = "confirmed"

        // Act
        val actual = PredictionClaimState.fromRaw(raw)

        // Assert
        assertThat(actual).isEqualTo(PredictionClaimState.CONFIRMED)
    }

    @Test
    fun `GIVEN an unknown raw state WHEN parsed THEN it is not terminal`() {
        // Arrange
        val raw = "SOMETHING_NEW"

        // Act
        val actual = PredictionClaimState.fromRaw(raw)

        // Assert
        assertThat(actual).isEqualTo(PredictionClaimState.IN_PROGRESS)
        assertThat(actual.isTerminal).isFalse()
    }

    @Test
    fun `GIVEN each state WHEN asked whether it is terminal THEN only IN_PROGRESS is not`() {
        // Arrange
        val states = PredictionClaimState.entries

        // Act
        val terminal = states.filter { it.isTerminal }

        // Assert
        assertThat(terminal)
            .containsExactly(PredictionClaimState.CONFIRMED, PredictionClaimState.FAILED)
    }
}