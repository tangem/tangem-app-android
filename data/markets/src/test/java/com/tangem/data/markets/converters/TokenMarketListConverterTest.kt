package com.tangem.data.markets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.compatibility.l2BlockchainsList
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.store.datasource.markets.models.response.TokenMarketListResponse
import com.tangem.domain.markets.TokenMarket
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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

    @Test
    fun `GIVEN a token restricted for the country WHEN convert THEN the flag is carried over`() {
        // Arrange
        val response = createResponse(createToken(id = "apple-xstock", isRestrictedForCountry = true))

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        assertThat(actual.tokens.single().isRestrictedForCountry).isTrue()
    }

    @Test
    fun `GIVEN the restriction flag is absent WHEN convert THEN the token counts as allowed`() {
        // Arrange — an older backend omits the field; defaulting to restricted would hide the whole list
        val response = createResponse(createToken(id = "ethereum", isRestrictedForCountry = null))

        // Act
        val actual = TokenMarketListConverter.convert(response)

        // Assert
        assertThat(actual.tokens.single().isRestrictedForCountry).isFalse()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldRate {

        @Test
        fun `GIVEN staking apy and Yield Mode apy WHEN convert THEN the staking maximum wins as sent`() {
            // Arrange — staking apy is used unshifted and beats Yield Mode, which alone would give 0.07
            val stakingOpportunities = listOf(
                createStakingOpportunity(apy = BigDecimal("0.03")),
                createStakingOpportunity(apy = BigDecimal("0.05")),
            )
            val response = createResponse(
                createToken(
                    id = "ethereum",
                    stakingOpportunities = stakingOpportunities,
                    maxYieldApy = BigDecimal("7"),
                ),
            )

            // Act
            val actual = TokenMarketListConverter.convert(response)

            // Assert
            assertThat(actual.tokens.single().yieldRate).isEqualTo(BigDecimal("0.05"))
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no positive staking apy and Yield Mode apy WHEN convert THEN yield rate is the percent shifted two places`(
            model: FallbackModel,
        ) {
            // Arrange
            val response = createResponse(
                createToken(
                    id = "ethereum",
                    stakingOpportunities = model.stakingOpportunities,
                    maxYieldApy = model.maxYieldApy,
                ),
            )

            // Act
            val actual = TokenMarketListConverter.convert(response)

            // Assert
            assertThat(actual.tokens.single().yieldRate).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN neither staking nor Yield Mode apy WHEN convert THEN yield rate is null`() {
            // Arrange
            val response = createResponse(
                createToken(id = "ethereum", stakingOpportunities = null, maxYieldApy = null),
            )

            // Act
            val actual = TokenMarketListConverter.convert(response)

            // Assert
            assertThat(actual.tokens.single().yieldRate).isNull()
        }

        @Test
        fun `GIVEN staking opportunities with null apy only WHEN convert THEN conversion throws as a known defect`() {
            // Arrange — no apy left after dropping nulls, so max() throws instead of falling back to Yield Mode
            val response = createResponse(
                createToken(
                    id = "ethereum",
                    stakingOpportunities = listOf(createStakingOpportunity(apy = null)),
                    maxYieldApy = BigDecimal("5.5"),
                ),
            )

            // Act
            val actual = runCatching { TokenMarketListConverter.convert(response) }

            // Assert
            assertThat(actual.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `GIVEN empty staking opportunities list WHEN convert THEN conversion throws as a known defect`() {
            // Arrange — same max() on an empty list as the all-null case
            val response = createResponse(
                createToken(
                    id = "ethereum",
                    stakingOpportunities = emptyList(),
                    maxYieldApy = BigDecimal("5.5"),
                ),
            )

            // Act
            val actual = runCatching { TokenMarketListConverter.convert(response) }

            // Assert
            assertThat(actual.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
        }

        private fun provideTestModels() = listOf(
            FallbackModel(
                stakingOpportunities = listOf(createStakingOpportunity(apy = BigDecimal("0"))),
                maxYieldApy = BigDecimal("5.5"),
                expected = BigDecimal("0.055"),
            ),
            FallbackModel(
                stakingOpportunities = listOf(createStakingOpportunity(apy = BigDecimal("-0.01"))),
                maxYieldApy = BigDecimal("5.5"),
                expected = BigDecimal("0.055"),
            ),
            FallbackModel(
                stakingOpportunities = null,
                maxYieldApy = BigDecimal("5.5"),
                expected = BigDecimal("0.055"),
            ),
        )
    }

    internal data class FallbackModel(
        val stakingOpportunities: List<TokenMarketListResponse.Token.StakingOpportunities>?,
        val maxYieldApy: BigDecimal,
        val expected: BigDecimal,
    )

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
        networks: List<TokenMarketListResponse.Token.Network>? = null,
        name: String = id,
        symbol: String = id.take(n = 3).uppercase(),
        isRestrictedForCountry: Boolean? = null,
        stakingOpportunities: List<TokenMarketListResponse.Token.StakingOpportunities>? = null,
        maxYieldApy: BigDecimal? = null,
    ) = TokenMarketListResponse.Token(
        id = id,
        name = name,
        symbol = symbol,
        currentPrice = BigDecimal.ONE,
        priceChangePercentage = null,
        marketRating = null,
        marketCap = null,
        isUnderMarketCapLimit = null,
        stakingOpportunities = stakingOpportunities,
        maxYieldApy = maxYieldApy,
        networks = networks,
        isRestrictedForCountry = isRestrictedForCountry,
    )

    private fun createStakingOpportunity(apy: BigDecimal?) = TokenMarketListResponse.Token.StakingOpportunities(
        id = 1,
        apy = apy,
        networkId = null,
        rewardType = null,
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