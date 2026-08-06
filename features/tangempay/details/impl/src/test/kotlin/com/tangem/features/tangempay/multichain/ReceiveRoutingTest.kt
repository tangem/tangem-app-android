package com.tangem.features.tangempay.multichain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.PaymentNetworkStatus
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class ReceiveRoutingTest {

    @Test
    fun `GIVEN multichain on and networks present WHEN shouldUseChooseNetwork THEN true`() {
        // Arrange
        val networks = listOf(available())

        // Act
        val result = shouldUseChooseNetwork(isMultichainEnabled = true, networks = networks)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN multichain off WHEN shouldUseChooseNetwork THEN false`() {
        // Arrange
        val networks = listOf(available())

        // Act
        val result = shouldUseChooseNetwork(isMultichainEnabled = false, networks = networks)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN multichain on but no networks WHEN shouldUseChooseNetwork THEN false`() {
        // Arrange + Act
        val result = shouldUseChooseNetwork(isMultichainEnabled = true, networks = emptyList())

        // Assert
        assertThat(result).isFalse()
    }

    private fun available() = PaymentNetworkStatus.Available(
        network = mockk(),
        depositAddress = "0xDEPOSIT",
        cryptoCurrencyStatuses = emptyList(),
    )
}