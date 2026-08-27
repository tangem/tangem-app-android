package com.tangem.features.tangempay.multichain.choosenetwork

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
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentChooseNetworkUMConverterTest {

    private val listener: ChooseNetworkListener = mockk(relaxed = true)
    private val onSelectNotIssued: (PaymentNetworkStatus.NotIssued) -> Unit = mockk(relaxed = true)
    private val converter = PaymentChooseNetworkUMConverter(listener = listener, onSelectNotIssued = onSelectNotIssued)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.common.ui.extensions.NetworkIconExtKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN mix of statuses WHEN convert THEN Fast way is Available+NotIssued and Other ways is Disabled, in input order`() {
        // Arrange
        val polygon = available(networkName = "Polygon", networkRawId = "polygon", address = "0xPOLY")
        val ethereum = notIssued(networkName = "Ethereum", networkRawId = "ethereum")
        val tron = disabled(networkName = "TRON", networkRawId = "tron")

        // Act
        val result = converter.convert(listOf(tron, polygon, ethereum))

        // Assert
        assertThat(result.fastWay.map { it.name }).containsExactly("Polygon", "Ethereum").inOrder()
        assertThat(result.otherWays.map { it.name }).containsExactly("TRON")
    }

    @Test
    fun `GIVEN status with no currencies WHEN convert THEN it is skipped from its section`() {
        // Arrange
        val empty = PaymentNetworkStatus.Disabled(
            network = network(networkName = "Empty", networkRawId = "empty"),
            cryptoCurrencies = emptyList(),
        )
        val tron = disabled(networkName = "TRON", networkRawId = "tron")

        // Act
        val result = converter.convert(listOf(empty, tron))

        // Assert
        assertThat(result.otherWays.map { it.name }).containsExactly("TRON")
    }

    @Test
    fun `GIVEN Available item WHEN onClick THEN listener onSelectAvailable is invoked with the network rawId`() {
        // Arrange
        val status = available(networkName = "Polygon", networkRawId = "polygon", address = "0xPOLY")

        // Act
        val result = converter.convert(listOf(status))
        result.fastWay.single().onClick()

        // Assert
        verify { listener.onSelectAvailable(networkRawId = "polygon") }
    }

    @Test
    fun `GIVEN NotIssued item WHEN onClick THEN onSelectNotIssued callback is invoked with the status`() {
        // Arrange
        val status = notIssued(networkName = "Ethereum", networkRawId = "ethereum")

        // Act
        val result = converter.convert(listOf(status))
        result.fastWay.single().onClick()

        // Assert
        verify { onSelectNotIssued(status) }
    }

    @Test
    fun `GIVEN Disabled item WHEN onClick THEN listener onSelectDisabled is invoked`() {
        // Arrange
        val status = disabled(networkName = "TRON", networkRawId = "tron")

        // Act
        val result = converter.convert(listOf(status))
        result.otherWays.single().onClick()

        // Assert
        verify { listener.onSelectDisabled() }
    }

    @Test
    fun `WHEN convert THEN dismiss delegates to listener onDismiss`() {
        // Act
        val result = converter.convert(emptyList())
        result.dismiss()

        // Assert
        verify { listener.onDismiss() }
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

    private fun available(
        networkName: String,
        networkRawId: String,
        address: String,
        symbol: String = "USDC",
    ): PaymentNetworkStatus.Available {
        val value: CryptoCurrencyStatus.Value = mockk()
        return PaymentNetworkStatus.Available(
            network = network(networkName, networkRawId),
            depositAddress = address,
            cryptoCurrencyStatuses = listOf(CryptoCurrencyStatus(currency = currency(symbol), value = value)),
        )
    }

    private fun notIssued(networkName: String, networkRawId: String): PaymentNetworkStatus.NotIssued {
        return PaymentNetworkStatus.NotIssued(
            network = network(networkName, networkRawId),
            cryptoCurrencies = listOf(currency("USDC")),
        )
    }

    private fun disabled(networkName: String, networkRawId: String): PaymentNetworkStatus.Disabled {
        return PaymentNetworkStatus.Disabled(
            network = network(networkName, networkRawId),
            cryptoCurrencies = listOf(currency("USDT")),
        )
    }

    private companion object {
        const val ICON_RES_ID = 1
    }
}