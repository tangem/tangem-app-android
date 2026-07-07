package com.tangem.blockchainsdk.compatibility

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.markets.models.response.TokenMarketInfoResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class L2NetworksTest {

    @Test
    fun `GIVEN ethereum info with networks WHEN applyL2Compatibility THEN missing L2 networks are appended`() {
        // Arrange
        val response = createInfoResponse(id = "ethereum", networks = listOf(createNetwork(networkId = "ethereum")))

        // Act
        val actual = response.applyL2Compatibility(coinId = "ethereum")

        // Assert
        val networkIds = actual.networks?.map(TokenMarketInfoResponse.Network::networkId)
        val expectedNetworkIds = listOf("ethereum") + l2BlockchainsList.map { it.toNetworkId() }
        assertThat(networkIds).containsExactlyElementsIn(expectedNetworkIds)
    }

    @Test
    fun `GIVEN ethereum info with backend-provided L2 network WHEN applyL2Compatibility THEN backend entry wins without duplicates`() {
        // Arrange
        val backendArbitrum = createNetwork(networkId = "arbitrum-one", decimalCount = 18)
        val response = createInfoResponse(
            id = "ethereum",
            networks = listOf(createNetwork(networkId = "ethereum"), backendArbitrum),
        )

        // Act
        val actual = response.applyL2Compatibility(coinId = "ethereum")

        // Assert
        val networks = actual.networks.orEmpty()
        assertThat(networks.map(TokenMarketInfoResponse.Network::networkId)).containsNoDuplicates()
        assertThat(networks.single { it.networkId == "arbitrum-one" }).isEqualTo(backendArbitrum)
    }

    @Test
    fun `GIVEN non-ethereum info WHEN applyL2Compatibility THEN networks stay unchanged`() {
        // Arrange
        val tetherNetworks = listOf(
            createNetwork(
                networkId = "ethereum",
                contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                decimalCount = 6,
            ),
        )
        val response = createInfoResponse(id = "tether", networks = tetherNetworks)

        // Act
        val actual = response.applyL2Compatibility(coinId = "tether")

        // Assert
        assertThat(actual.networks).isEqualTo(tetherNetworks)
    }

    private fun createInfoResponse(
        id: String,
        networks: List<TokenMarketInfoResponse.Network>?,
    ) = TokenMarketInfoResponse(
        id = id,
        name = id,
        symbol = id.take(n = 3).uppercase(),
        currentPrice = BigDecimal.ONE,
        priceChangePercentage = null,
        networks = networks,
        shortDescription = null,
        fullDescription = null,
        insights = null,
        metrics = null,
        securityData = null,
        links = null,
        pricePerformance = null,
        exchangesAmount = null,
    )

    private fun createNetwork(
        networkId: String,
        contractAddress: String? = null,
        decimalCount: Int? = null,
    ) = TokenMarketInfoResponse.Network(
        networkId = networkId,
        exchangeable = false,
        contractAddress = contractAddress,
        decimalCount = decimalCount,
    )
}