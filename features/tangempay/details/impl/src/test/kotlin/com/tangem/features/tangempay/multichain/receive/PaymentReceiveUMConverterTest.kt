package com.tangem.features.tangempay.multichain.receive

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.tangempay.details.impl.R
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
internal class PaymentReceiveUMConverterTest {

    private val onCopy: () -> Unit = mockk(relaxed = true)
    private val onShowQr: () -> Unit = mockk(relaxed = true)
    private val onShare: () -> Unit = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    private val converter = PaymentReceiveUMConverter(
        onCopy = onCopy,
        onShowQr = onShowQr,
        onShare = onShare,
        onDismiss = onDismiss,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.common.ui.extensions.NetworkIconExtKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN multiple tokens WHEN convert THEN UM has one icon per token, in order`() {
        // Arrange
        val usdc = token(symbol = "USDC")
        val usdt = token(symbol = "USDT")
        val input = PaymentReceiveUMConverter.Input(
            networkName = "Solana",
            address = "0xADDRESS",
            currencies = listOf(usdc, usdt),
        )

        // Act
        val result = converter.convert(input)

        // Assert
        assertThat(result.tokens.map { it.symbol }).containsExactly("USDC", "USDT").inOrder()
    }

    @Test
    fun `GIVEN multiple tokens WHEN convert THEN warning mentions every token symbol and the network name`() {
        // Arrange
        val input = PaymentReceiveUMConverter.Input(
            networkName = "Solana",
            address = "0xADDRESS",
            currencies = listOf(token(symbol = "USDC"), token(symbol = "USDT")),
        )

        // Act
        val result = converter.convert(input)
        val warning = result.warning as TextReference.Res

        // Assert
        assertThat(warning.id).isEqualTo(R.string.receive_bottom_sheet_warning_title)
        assertThat(warning.formatArgs).containsExactly("USDC, USDT", "Solana").inOrder()
    }

    @Test
    fun `GIVEN multiple tokens WHEN convert THEN tokensOnNetworkLabel mentions every token and the network`() {
        // Arrange
        val input = PaymentReceiveUMConverter.Input(
            networkName = "Solana",
            address = "0xADDRESS",
            currencies = listOf(token(symbol = "USDC"), token(symbol = "USDT")),
        )

        // Act
        val label = converter.convert(input).tokensOnNetworkLabel as TextReference.Res

        // Assert
        assertThat(label.id).isEqualTo(R.string.receive_bottom_sheet_warning_message_compact)
        assertThat(label.formatArgs).containsExactly("USDC, USDT", "Solana").inOrder()
    }

    @Test
    fun `GIVEN an address WHEN convert THEN UM carries it unchanged`() {
        // Arrange
        val input = PaymentReceiveUMConverter.Input(
            networkName = "Solana",
            address = "0xDEPOSIT_ADDRESS",
            currencies = listOf(token(symbol = "USDC")),
        )

        // Act
        val result = converter.convert(input)

        // Assert
        assertThat(result.address).isEqualTo("0xDEPOSIT_ADDRESS")
    }

    @Test
    fun `WHEN convert THEN UM callbacks delegate to the constructor callbacks`() {
        // Arrange
        val input = PaymentReceiveUMConverter.Input(
            networkName = "Solana",
            address = "0xADDRESS",
            currencies = listOf(token(symbol = "USDC")),
        )

        // Act
        val result = converter.convert(input)
        result.onCopy()
        result.onShowQr()
        result.onShare()
        result.onDismiss()

        // Assert
        verify {
            onCopy()
            onShowQr()
            onShare()
            onDismiss()
        }
    }

    private fun token(symbol: String): CryptoCurrency.Token {
        val network: Network = mockk()
        every { network.isTestnet } returns false
        val currency: CryptoCurrency.Token = mockk()
        every { currency.symbol } returns symbol
        every { currency.network } returns network
        every { currency.iconUrl } returns null
        every { currency.isCustom } returns false
        every { currency.contractAddress } returns "0xCONTRACT"
        every { currency.networkIconResId } returns ICON_RES_ID
        return currency
    }

    private companion object {
        const val ICON_RES_ID = 1
    }
}