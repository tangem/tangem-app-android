package com.tangem.features.jointaccount.supportednetworks.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.domain.models.network.Network
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM.NetworkItemUM
import org.junit.jupiter.api.Test

internal class SupportedNetworkItemConverterTest {

    @Test
    fun `GIVEN raw ids WHEN convert THEN output preserves input order`() {
        // Arrange
        val rawIds = listOf(
            Network.RawID(value = "polygon-pos"),
            Network.RawID(value = "ethereum"),
            Network.RawID(value = "binance-smart-chain"),
        )

        // Act
        val actual = SupportedNetworkItemConverter.convert(rawIds)

        // Assert
        assertThat(actual.map(NetworkItemUM::id))
            .containsExactly("polygon-pos", "ethereum", "binance-smart-chain")
            .inOrder()
    }

    @Test
    fun `GIVEN known raw ids WHEN convert THEN name symbol and icon are mapped`() {
        // Arrange
        val rawIds = listOf(
            Network.RawID(value = "ethereum"),
            Network.RawID(value = "binance-smart-chain"),
        )
        val expected = listOf(
            NetworkItemUM(
                id = "ethereum",
                name = Blockchain.Ethereum.fullName,
                symbol = Blockchain.Ethereum.currency,
                iconResId = getActiveIconRes(Blockchain.Ethereum),
            ),
            NetworkItemUM(
                id = "binance-smart-chain",
                name = Blockchain.BSC.fullName,
                symbol = Blockchain.BSC.currency,
                iconResId = getActiveIconRes(Blockchain.BSC),
            ),
        )

        // Act
        val actual = SupportedNetworkItemConverter.convert(rawIds)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unknown raw id WHEN convert THEN it is skipped`() {
        // Arrange
        val rawIds = listOf(
            Network.RawID(value = "ethereum"),
            Network.RawID(value = "unknown-network"),
        )

        // Act
        val actual = SupportedNetworkItemConverter.convert(rawIds)

        // Assert
        assertThat(actual.map(NetworkItemUM::id)).containsExactly("ethereum")
    }
}