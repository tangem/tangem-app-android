package com.tangem.data.pay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class PaymentNetworkBlockchainResolverTest {

    @Test
    fun `GIVEN evm chain ids WHEN resolve THEN maps via fromChainId`() {
        assertThat(resolve("polygon", 137L)).isEqualTo(Blockchain.Polygon)
        assertThat(resolve("bsc", 56L)).isEqualTo(Blockchain.BSC)
        assertThat(resolve("base", 8453L)).isEqualTo(Blockchain.Base)
        assertThat(resolve("arbitrum", 42161L)).isEqualTo(Blockchain.Arbitrum)
        assertThat(resolve("ethereum", 1L)).isEqualTo(Blockchain.Ethereum)
    }

    @Test
    fun `GIVEN tron name WHEN resolve THEN maps to Tron even though not EVM`() {
        assertThat(resolve("tron", 728126428L)).isEqualTo(Blockchain.Tron)
    }

    @Test
    fun `GIVEN unknown network WHEN resolve THEN null`() {
        assertThat(resolve("mystery", -1L)).isNull()
    }

    @Test
    fun `GIVEN chainId outside Int range WHEN resolve THEN null instead of wrapping to a colliding chain id`() {
        // 4294967433 = 137 (Polygon) + 2^32. Without an overflow guard, chainId.toInt() would silently
        // wrap this back down to 137 and incorrectly resolve to Blockchain.Polygon.
        assertThat(resolve("x", 4294967433L)).isNull()
    }

    @Test
    fun `GIVEN isTestnet true WHEN resolve THEN returns the testnet variant of the blockchain`() {
        // Mainnet chain id + testnet flag -> testnet variant via getTestnetVersion().
        assertThat(resolve("polygon", 137L, isTestnet = true)).isEqualTo(Blockchain.PolygonTestnet)
        // Non-EVM launch network resolved by name is testnet-ified too.
        assertThat(resolve("tron", 728126428L, isTestnet = true)).isEqualTo(Blockchain.TronTestnet)
        // A chain id that already resolves to a testnet variant stays that variant (idempotent).
        assertThat(resolve("base", 84532L, isTestnet = true)).isEqualTo(Blockchain.BaseTestnet)
    }

    private fun resolve(name: String, chainId: Long, isTestnet: Boolean = false): Blockchain? =
        PaymentNetworkBlockchainResolver.blockchainFor(name = name, chainId = chainId, isTestnet = isTestnet)
}