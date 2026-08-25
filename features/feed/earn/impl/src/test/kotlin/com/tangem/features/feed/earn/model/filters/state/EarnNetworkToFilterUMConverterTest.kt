package com.tangem.features.feed.earn.model.filters.state

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.features.feed.earn.createEarnNetwork
import org.junit.jupiter.api.Test

internal class EarnNetworkToFilterUMConverterTest {

    private var clicked: EarnNetwork? = null

    private fun converter(selectedNetworkId: String? = null) = EarnNetworkToFilterUMConverter(
        selectedNetworkId = selectedNetworkId,
        onClick = { clicked = it },
    )

    @Test
    fun `GIVEN a network WHEN converted THEN its id name and symbol reach the row`() {
        // Arrange
        val network = createEarnNetwork(networkId = "solana", fullName = "Solana", symbol = "SOL")

        // Act
        val actual = converter().convert(network)

        // Assert
        assertThat(actual.id).isEqualTo("solana")
        assertThat(actual.name).isEqualTo(stringReference("Solana"))
        assertThat(actual.symbol).isEqualTo("SOL")
    }

    @Test
    fun `GIVEN the selected network WHEN converted THEN only its row is selected`() {
        // Arrange
        val selected = createEarnNetwork(networkId = "ethereum")
        val other = createEarnNetwork(networkId = "solana")

        // Act
        val actual = converter(selectedNetworkId = "ethereum").convertList(listOf(selected, other))

        // Assert
        assertThat(actual.map { it.isSelected }).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN nothing selected WHEN converted THEN no row is selected`() {
        // Act
        val actual = converter(selectedNetworkId = null).convert(createEarnNetwork(networkId = "ethereum"))

        // Assert
        assertThat(actual.isSelected).isFalse()
    }

    @Test
    fun `GIVEN a known network id WHEN converted THEN the row carries that blockchain's icon`() {
        // Act
        val actual = converter().convert(createEarnNetwork(networkId = "solana", symbol = "SOL"))

        // Assert — the icon is resolved from the network id, not from the symbol or the name
        assertThat(actual.iconRes).isEqualTo(getActiveIconRes(Blockchain.Solana))
    }

    @Test
    fun `GIVEN an unknown network id WHEN converted THEN the row falls back to the unknown icon`() {
        // Act
        val actual = converter().convert(createEarnNetwork(networkId = "not-a-chain"))

        // Assert
        assertThat(actual.iconRes).isEqualTo(getActiveIconRes(Blockchain.Unknown))
    }

    @Test
    fun `GIVEN a row WHEN it is clicked THEN the network behind it is reported`() {
        // Arrange
        val network = createEarnNetwork(networkId = "solana")

        // Act
        converter().convert(network).onClick()

        // Assert
        assertThat(clicked).isEqualTo(network)
    }
}