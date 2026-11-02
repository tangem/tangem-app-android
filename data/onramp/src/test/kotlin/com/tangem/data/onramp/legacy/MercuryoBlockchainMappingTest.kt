package com.tangem.data.onramp.legacy

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class MercuryoBlockchainMappingTest {

    @Test
    fun `GIVEN supported blockchains WHEN mercuryo network requested THEN provider ids are returned`() {
        // Assert
        assertThat(
            mapOf(
                Blockchain.Algorand to "ALGORAND",
                Blockchain.BSC to "BINANCESMARTCHAIN",
                Blockchain.Bitcoin to "BITCOIN",
                Blockchain.Ethereum to "ETHEREUM",
                Blockchain.Near to "NEAR_PROTOCOL",
                Blockchain.TON to "NEWTON",
                Blockchain.Polygon to "POLYGON",
                Blockchain.XRP to "RIPPLE",
                Blockchain.Solana to "SOLANA",
                Blockchain.ZkSyncEra to "ZKSYNC",
                Blockchain.Base to "BASE",
            ).mapValues { it.key.mercuryoNetwork },
        ).containsExactlyEntriesIn(
            mapOf(
                Blockchain.Algorand to "ALGORAND",
                Blockchain.BSC to "BINANCESMARTCHAIN",
                Blockchain.Bitcoin to "BITCOIN",
                Blockchain.Ethereum to "ETHEREUM",
                Blockchain.Near to "NEAR_PROTOCOL",
                Blockchain.TON to "NEWTON",
                Blockchain.Polygon to "POLYGON",
                Blockchain.XRP to "RIPPLE",
                Blockchain.Solana to "SOLANA",
                Blockchain.ZkSyncEra to "ZKSYNC",
                Blockchain.Base to "BASE",
            ),
        )
    }

    @Test
    fun `GIVEN testnet blockchain WHEN mercuryo network requested THEN null is returned`() {
        // Act
        val actual = Blockchain.EthereumTestnet.mercuryoNetwork

        // Assert
        assertThat(actual).isNull()
    }
}