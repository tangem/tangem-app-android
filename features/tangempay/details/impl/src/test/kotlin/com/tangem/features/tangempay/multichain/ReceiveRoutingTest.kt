package com.tangem.features.tangempay.multichain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.PaymentNetworkStatus
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class ReceiveRoutingTest {

    @Test
    fun `GIVEN networks present WHEN shouldUseChooseNetwork THEN true`() {
        // Arrange
        val networks = listOf(available())

        // Act
        val result = shouldUseChooseNetwork(networks)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN no networks WHEN shouldUseChooseNetwork THEN false`() {
        // Act
        val result = shouldUseChooseNetwork(emptyList())

        // Assert
        assertThat(result).isFalse()
    }

    private fun available() = PaymentNetworkStatus.Available(
        network = mockk(),
        depositAddress = "0xDEPOSIT",
        chainId = 137L,
        cryptoCurrencyStatuses = emptyList(),
    )
}