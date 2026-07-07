package com.tangem.data.markets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.compatibility.l2BlockchainsList
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.markets.models.response.TokenMarketListResponse
import com.tangem.domain.markets.TokenMarket
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TokenMarketListConverterTest {

    @Test
    fun `GIVEN ethereum coin with networks WHEN convert THEN L2 networks are appended`() {
        // Arrange
        val response = createResponse(
            createToken(id = "ethereum", networks = listOf(createNetwork(networkId = "ethereum"))),
        )

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        val networkIds = actual.tokens.single().networks?.map(TokenMarket.Network::networkId)
        val expectedNetworkIds = listOf("ethereum") + l2BlockchainsList.map { it.toNetworkId() }
        assertThat(networkIds).containsExactlyElementsIn(expectedNetworkIds)
        assertThat(networkIds).containsAtLeast("arbitrum-one", "optimistic-ethereum", "base")
    }

    @Test
    fun `GIVEN ethereum coin with networks WHEN convert THEN appended L2 networks are native coin entries`() {
        // Arrange
        val response = createResponse(
            createToken(id = "ethereum", networks = listOf(createNetwork(networkId = "ethereum"))),
        )

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        val l2Networks = actual.tokens.single().networks.orEmpty().filter { it.networkId != "ethereum" }
        assertThat(l2Networks).isNotEmpty()
        assertThat(l2Networks.mapNotNull(TokenMarket.Network::contractAddress)).isEmpty()
    }

    @Test
    fun `GIVEN ethereum coin with backend-provided L2 network WHEN convert THEN backend entry wins without duplicates`() {
        // Arrange
        val backendArbitrum = createNetwork(networkId = "arbitrum-one", decimalCount = 18)
        val response = createResponse(
            createToken(id = "ethereum", networks = listOf(createNetwork(networkId = "ethereum"), backendArbitrum)),
        )

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        val networks = actual.tokens.single().networks.orEmpty()
        assertThat(networks.map(TokenMarket.Network::networkId)).containsNoDuplicates()
        val arbitrum = networks.single { it.networkId == "arbitrum-one" }
        assertThat(arbitrum.decimalCount).isEqualTo(18)
    }

    @Test
    fun `GIVEN non-ethereum token with networks WHEN convert THEN networks stay unchanged`() {
        // Arrange
        val tetherNetworks = listOf(
            createNetwork(
                networkId = "ethereum",
                contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                decimalCount = 6,
            ),
            createNetwork(
                networkId = "tron",
                contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                decimalCount = 6,
            ),
        )
        val response = createResponse(createToken(id = "tether", networks = tetherNetworks))

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        val expected = tetherNetworks.map { network ->
            TokenMarket.Network(
                networkId = network.networkId,
                contractAddress = network.contractAddress,
                decimalCount = network.decimalCount,
            )
        }
        assertThat(actual.tokens.single().networks).isEqualTo(expected)
    }

    @Test
    fun `GIVEN ethereum coin without networks WHEN convert THEN networks stay null`() {
        // Arrange
        val response = createResponse(createToken(id = "ethereum", networks = null))

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        assertThat(actual.tokens.single().networks).isNull()
    }

    private fun createResponse(vararg tokens: TokenMarketListResponse.Token) = TokenMarketListResponse(
        imageHost = "https://img.tangem.org/",
        tokens = tokens.toList(),
        total = tokens.size,
        limit = 20,
        offset = 0,
        timestamp = 1L,
        summary = null,
    )

    private fun createToken(
        id: String,
        networks: List<TokenMarketListResponse.Token.Network>?,
        name: String = id,
        symbol: String = id.take(n = 3).uppercase(),
    ) = TokenMarketListResponse.Token(
        id = id,
        name = name,
        symbol = symbol,
        currentPrice = BigDecimal.ONE,
        priceChangePercentage = null,
        marketRating = null,
        marketCap = null,
        isUnderMarketCapLimit = null,
        stakingOpportunities = null,
        maxYieldApy = null,
        networks = networks,
    )

    private fun createNetwork(
        networkId: String,
        contractAddress: String? = null,
        decimalCount: Int? = null,
    ) = TokenMarketListResponse.Token.Network(
        networkId = networkId,
        contractAddress = contractAddress,
        decimalCount = decimalCount,
    )
}