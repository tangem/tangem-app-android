package com.tangem.domain.card.common.tangempay

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TangemPayUtilitiesTest {

    @Test
    fun `GIVEN checksummed address WHEN moneyAccountDepositAddressMessage THEN address is lowercased`() {
        // Arrange
        val address = "0xAbC123dEF456"

        // Act
        val actual = TangemPayUtilities.moneyAccountDepositAddressMessage(depositAddress = address)

        // Assert
        assertThat(actual).isEqualTo("Banking account deposit address 0xabc123def456")
    }
}