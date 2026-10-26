package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

internal class PolymarketDepositOptionsTest {

    @Test
    fun `GIVEN the bridge lists every catalogued asset in reverse WHEN options read THEN they follow the catalogue`() {
        // Arrange
        val requisites = PolymarketDepositRequisites(networks = PolymarketDepositAsset.CATALOG.reversed().map(::listed))

        // Act
        val actual = requisites.options.map { it.asset }

        // Assert
        assertThat(actual).containsExactlyElementsIn(PolymarketDepositAsset.CATALOG).inOrder()
        assertThat(actual.first()).isEqualTo(PolymarketDepositAsset.POLYGON_USDC)
    }

    @Test
    fun `GIVEN an EVM token in checksum case WHEN options read THEN it matches its catalogue row`() {
        // Arrange
        val requisites = PolymarketDepositRequisites(
            networks = listOf(listed(ARBITRUM_USDC, tokenAddress = ARBITRUM_USDC.contractAddress.uppercase())),
        )

        // Act
        val actual = requisites.options.single()

        // Assert
        assertThat(actual.asset).isEqualTo(ARBITRUM_USDC)
        assertThat(actual.target.address).isEqualTo(EVM_ADDRESS)
    }

    @Test
    fun `GIVEN the Solana mint in another case WHEN options read THEN it is not offered`() {
        // Arrange
        val requisites = PolymarketDepositRequisites(
            networks = listOf(listed(SOLANA_USDC, tokenAddress = SOLANA_USDC.contractAddress.lowercase())),
        )

        // Act
        val actual = requisites.options

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN Solana USDC WHEN options read THEN it carries the Solana address`() {
        // Arrange
        val requisites = PolymarketDepositRequisites(networks = listOf(listed(SOLANA_USDC)))

        // Act
        val actual = requisites.options.single()

        // Assert
        assertThat(actual.target.address).isEqualTo(SVM_ADDRESS)
    }

    @ParameterizedTest
    @MethodSource("provideUnofferedListings")
    fun `GIVEN a listing that disagrees with the catalogue WHEN options read THEN it is not offered`(
        network: PolymarketDepositNetwork,
    ) {
        // Arrange
        val requisites = PolymarketDepositRequisites(networks = listOf(network))

        // Act
        val actual = requisites.options

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a wallet token on the catalogued chain WHEN isAssetOf checked THEN only the same contract matches`() {
        // Arrange
        val usdc = token(Blockchain.Arbitrum, ARBITRUM_USDC.contractAddress.uppercase())
        val other = token(Blockchain.Arbitrum, "0xfd086bc7cd5c481dcc9c85ebe478a1c0b69fcbb9")
        val onAnotherChain = token(Blockchain.Base, ARBITRUM_USDC.contractAddress)

        // Act
        val actual = listOf(usdc, other, onAnotherChain).map(ARBITRUM_USDC::isAssetOf)

        // Assert
        assertThat(actual).containsExactly(true, false, false).inOrder()
    }

    private fun token(blockchain: Blockchain, contractAddress: String): CryptoCurrency.Token = mockk {
        every { network.rawId } returns blockchain.toNetworkId()
        every { this@mockk.contractAddress } returns contractAddress
    }

    private companion object {

        const val EVM_ADDRESS = "0xbridge"
        const val SVM_ADDRESS = "So1Bridge"

        val ARBITRUM_USDC = PolymarketDepositAsset.CATALOG.first { it.blockchain == Blockchain.Arbitrum }
        val BSC_USDC = PolymarketDepositAsset.CATALOG.first { it.blockchain == Blockchain.BSC }
        val SOLANA_USDC = PolymarketDepositAsset.CATALOG.first { it.blockchain == Blockchain.Solana }

        fun listed(
            asset: PolymarketDepositAsset,
            tokenAddress: String = asset.contractAddress,
            decimals: Int = asset.decimals,
            minCheckoutUsd: BigDecimal? = BigDecimal("2"),
            chainId: String = asset.chainId,
        ): PolymarketDepositNetwork {
            val family = requireNotNull(PolymarketChainFamily.of(asset.chainId))
            return PolymarketDepositNetwork(
                chainId = chainId,
                chainName = asset.blockchain.name,
                family = family,
                address = if (family == PolymarketChainFamily.SVM) SVM_ADDRESS else EVM_ADDRESS,
                tokens = listOf(
                    PolymarketDepositToken(
                        symbol = "USDC",
                        address = tokenAddress,
                        decimals = decimals,
                        minCheckoutUsd = minCheckoutUsd,
                    ),
                ),
            )
        }

        @JvmStatic
        fun provideUnofferedListings() = listOf(
            Arguments.of(listed(ARBITRUM_USDC, minCheckoutUsd = null)),
            Arguments.of(listed(BSC_USDC, decimals = 6)),
            Arguments.of(listed(ARBITRUM_USDC, tokenAddress = "0xff970a61a04b1ca14834a43f5de4533ebddb5cc8")),
            Arguments.of(listed(ARBITRUM_USDC, chainId = "8453")),
            Arguments.of(listed(ARBITRUM_USDC.copy(chainId = "4663"))),
        )
    }
}