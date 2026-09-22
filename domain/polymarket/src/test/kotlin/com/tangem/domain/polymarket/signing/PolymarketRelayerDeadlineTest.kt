package com.tangem.domain.polymarket.signing

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PolymarketRelayerDeadlineTest {

    @Test
    fun `GIVEN a moment in millis WHEN fromNow THEN deadline is ten minutes later in whole seconds`() {
        // Arrange
        val nowMillis = 1_735_689_600_999L

        // Act
        val deadline = PolymarketRelayerDeadline.fromNow(nowMillis)

        // Assert
        assertThat(deadline).isEqualTo("1735690200")
    }

    @Test
    fun `GIVEN a deadline one second ahead WHEN checked THEN it is not expired`() {
        // Arrange
        val nowMillis = 1_787_346_200_000L

        // Act
        val expired = PolymarketRelayerDeadline.isExpired(deadline = "1787346201", nowMillis = nowMillis)

        // Assert
        assertThat(expired).isFalse()
    }

    @Test
    fun `GIVEN a malformed deadline WHEN checked THEN it counts as expired`() {
        // Arrange
        val deadline = "soon"

        // Act
        val expired = PolymarketRelayerDeadline.isExpired(deadline = deadline)

        // Assert
        assertThat(expired).isTrue()
    }
}