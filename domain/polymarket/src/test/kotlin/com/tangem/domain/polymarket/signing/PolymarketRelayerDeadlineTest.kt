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
}