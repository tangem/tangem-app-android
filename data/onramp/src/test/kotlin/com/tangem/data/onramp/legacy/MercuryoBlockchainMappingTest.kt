package com.tangem.data.onramp.legacy

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

internal class MercuryoBlockchainMappingTest {

    @ParameterizedTest
    @CsvSource(
        "Algorand,ALGORAND", "Arbitrum,ARBITRUM", "Avalanche,AVALANCHE", "BSC,BINANCESMARTCHAIN",
        "Bitcoin,BITCOIN", "BitcoinCash,BITCOINCASH", "Cardano,CARDANO", "Cosmos,COSMOS",
        "Dash,DASH", "Dogecoin,DOGECOIN", "Ethereum,ETHEREUM", "Fantom,FANTOM", "Kusama,KUSAMA",
        "Litecoin,LITECOIN", "Near,NEAR_PROTOCOL", "TON,NEWTON", "Optimism,OPTIMISM",
        "Polkadot,POLKADOT", "Polygon,POLYGON", "XRP,RIPPLE", "Solana,SOLANA", "Stellar,STELLAR",
        "Tezos,TEZOS", "Tron,TRON", "ZkSyncEra,ZKSYNC", "Base,BASE",
    )
    fun `GIVEN supported blockchain WHEN network requested THEN provider identifier returned`(
        blockchain: Blockchain,
        expected: String,
    ) {
        assertThat(blockchain.mercuryoNetwork).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(
        value = Blockchain::class,
        names = ["EthereumTestnet", "BitcoinTestnet", "Binance", "Unknown", "BaseTestnet", "Sui"],
    )
    fun `GIVEN unsupported blockchain WHEN network requested THEN no provider network returned`(blockchain: Blockchain) {
        assertThat(blockchain.mercuryoNetwork).isNull()
    }
}