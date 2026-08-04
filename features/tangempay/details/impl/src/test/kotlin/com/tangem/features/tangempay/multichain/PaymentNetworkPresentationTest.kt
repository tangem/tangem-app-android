package com.tangem.features.tangempay.multichain

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.iconResId
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentNetworkPresentationTest {

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.common.ui.extensions.NetworkIconExtKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN Available status WHEN toRowData THEN maps identity from network and tokens from currencies`() {
        // Arrange
        val status = PaymentNetworkStatus.Available(
            network = network(networkName = "Polygon", networkRawId = "polygon"),
            depositAddress = "0xDEPOSIT",
            cryptoCurrencyStatuses = listOf(status(currency("USDC")), status(currency("USDT"))),
        )

        // Act
        val result = status.toRowData()

        // Assert
        assertThat(result?.id).isEqualTo("polygon")
        assertThat(result?.name).isEqualTo("Polygon")
        assertThat(result?.tokensLabel).isEqualTo("USDC, USDT")
        assertThat(result?.iconResId).isEqualTo(ICON_RES_ID)
    }

    @Test
    fun `GIVEN NotIssued status WHEN toRowData THEN maps identity from network and tokens from currencies`() {
        // Arrange
        val status = PaymentNetworkStatus.NotIssued(
            network = network(networkName = "Ethereum", networkRawId = "ethereum"),
            cryptoCurrencies = listOf(currency("USDC")),
        )

        // Act
        val result = status.toRowData()

        // Assert
        assertThat(result?.name).isEqualTo("Ethereum")
        assertThat(result?.tokensLabel).isEqualTo("USDC")
        assertThat(result?.iconResId).isEqualTo(ICON_RES_ID)
    }

    @Test
    fun `GIVEN Disabled status WHEN toRowData THEN maps name, tokens and icon`() {
        // Arrange
        val status = PaymentNetworkStatus.Disabled(
            network = network(networkName = "TRON", networkRawId = "tron"),
            cryptoCurrencies = listOf(currency("USDT")),
        )

        // Act
        val result = status.toRowData()

        // Assert
        assertThat(result?.name).isEqualTo("TRON")
        assertThat(result?.tokensLabel).isEqualTo("USDT")
        assertThat(result?.iconResId).isEqualTo(ICON_RES_ID)
    }

    @Test
    fun `GIVEN status with no currencies WHEN toRowData THEN returns null`() {
        // Arrange
        val status = PaymentNetworkStatus.Disabled(
            network = network(networkName = "TRON", networkRawId = "tron"),
            cryptoCurrencies = emptyList(),
        )

        // Act
        val result = status.toRowData()

        // Assert
        assertThat(result).isNull()
    }

    private fun network(networkName: String, networkRawId: String): Network {
        val network: Network = mockk {
            every { name } returns networkName
            every { rawId } returns networkRawId
        }
        every { network.iconResId } returns ICON_RES_ID
        return network
    }

    private fun currency(symbol: String): CryptoCurrency.Token {
        val token: CryptoCurrency.Token = mockk()
        every { token.symbol } returns symbol
        return token
    }

    private fun status(currency: CryptoCurrency): CryptoCurrencyStatus {
        val value: CryptoCurrencyStatus.Value = mockk()
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    private companion object {
        const val ICON_RES_ID = 4242
    }
}