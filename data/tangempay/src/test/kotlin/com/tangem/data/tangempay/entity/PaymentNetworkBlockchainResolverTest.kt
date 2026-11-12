package com.tangem.data.tangempay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentNetworkBlockchainResolverTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN supported network WHEN resolve THEN maps to its blockchain`(model: ResolveModel) {
        // Act
        val actual = resolve(model.name, model.chainId, isTestnet = model.isTestnet)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideUnresolvableModels")
    fun `GIVEN unknown network or chain id outside Int range WHEN resolve THEN null`(model: UnresolvableModel) {
        // Act
        val actual = resolve(model.name, model.chainId)

        // Assert
        assertThat(actual).isNull()
    }

    private fun resolve(name: String, chainId: Long, isTestnet: Boolean = false): Blockchain? =
        PaymentNetworkBlockchainResolver.blockchainFor(name = name, chainId = chainId, isTestnet = isTestnet)

    private fun provideTestModels() = listOf(
        ResolveModel(name = "polygon", chainId = 137L, expected = Blockchain.Polygon),
        ResolveModel(name = "bsc", chainId = 56L, expected = Blockchain.BSC),
        ResolveModel(name = "base", chainId = 8453L, expected = Blockchain.Base),
        ResolveModel(name = "arbitrum", chainId = 42161L, expected = Blockchain.Arbitrum),
        ResolveModel(name = "ethereum", chainId = 1L, expected = Blockchain.Ethereum),
        ResolveModel(name = "tron", chainId = 728126428L, expected = Blockchain.Tron),
        // Mainnet chain id + testnet flag -> testnet variant via getTestnetVersion().
        ResolveModel(name = "polygon", chainId = 137L, isTestnet = true, expected = Blockchain.PolygonTestnet),
        // Non-EVM launch network resolved by name is testnet-ified too.
        ResolveModel(name = "tron", chainId = 728126428L, isTestnet = true, expected = Blockchain.TronTestnet),
        // A chain id that already resolves to a testnet variant stays that variant (idempotent).
        ResolveModel(name = "base", chainId = 84532L, isTestnet = true, expected = Blockchain.BaseTestnet),
    )

    private fun provideUnresolvableModels() = listOf(
        UnresolvableModel(name = "mystery", chainId = -1L),
        // 4294967433 = 137 (Polygon) + 2^32. Without an overflow guard, chainId.toInt() would silently
        // wrap this back down to 137 and incorrectly resolve to Blockchain.Polygon.
        UnresolvableModel(name = "x", chainId = 4294967433L),
    )

    internal data class ResolveModel(
        val name: String,
        val chainId: Long,
        val isTestnet: Boolean = false,
        val expected: Blockchain,
    )

    internal data class UnresolvableModel(val name: String, val chainId: Long)
}