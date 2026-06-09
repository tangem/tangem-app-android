package com.tangem.common.ui.extensions

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BlockchainIconsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetActiveIconRes {

        @ParameterizedTest
        @ProvideTestModels
        fun returnsCorrectDrawable(model: TestModel) {
            assertThat(getActiveIconRes(model.input)).isEqualTo(model.expected)
        }

        @Test
        fun returnsAlertDrawableForUnknownId() {
            assertThat(getActiveIconRes(Blockchain.Unknown)).isEqualTo(R.drawable.ic_alert_24)
        }

        @Suppress("CyclomaticComplexMethod", "LongMethod")
        private fun provideTestModels() = Blockchain.entries.map { blockchain ->
            val expected = when (blockchain) {
                Blockchain.Alephium, Blockchain.AlephiumTestnet -> R.drawable.img_alephium_22
                Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> R.drawable.img_azero_22
                Blockchain.Algorand, Blockchain.AlgorandTestnet -> R.drawable.img_algorand_22
                Blockchain.ApeChain, Blockchain.ApeChainTestnet -> R.drawable.img_apecoin_22
                Blockchain.Aptos, Blockchain.AptosTestnet -> R.drawable.img_aptos_22
                Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> R.drawable.img_arbitrum_22
                Blockchain.ArbitrumNova -> R.drawable.img_arbitrum_nova_22
                Blockchain.Areon, Blockchain.AreonTestnet -> R.drawable.img_areon_22
                Blockchain.Aurora, Blockchain.AuroraTestnet -> R.drawable.img_aurora_22
                Blockchain.Avalanche, Blockchain.AvalancheTestnet -> R.drawable.img_avalanche_22
                Blockchain.BSC, Blockchain.BSCTestnet,
                Blockchain.Binance, Blockchain.BinanceTestnet,
                -> R.drawable.img_bsc_22
                Blockchain.Base, Blockchain.BaseTestnet -> R.drawable.img_base_22
                Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> R.drawable.img_btc_22
                Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> R.drawable.img_btc_cash_22
                Blockchain.Bitrock, Blockchain.BitrockTestnet -> R.drawable.img_bitrock_22
                Blockchain.Bittensor -> R.drawable.img_bittensor_22
                Blockchain.Blast, Blockchain.BlastTestnet -> R.drawable.img_blast_22
                Blockchain.Canxium -> R.drawable.img_canxium_22
                Blockchain.Cardano -> R.drawable.img_cardano_22
                Blockchain.Casper, Blockchain.CasperTestnet -> R.drawable.img_casper_22
                Blockchain.Chia, Blockchain.ChiaTestnet -> R.drawable.img_chia_22
                Blockchain.Chiliz, Blockchain.ChilizTestnet -> R.drawable.img_chiliz_22
                Blockchain.Clore -> R.drawable.img_clore_22
                Blockchain.Core, Blockchain.CoreTestnet -> R.drawable.img_core_22
                Blockchain.Cosmos, Blockchain.CosmosTestnet -> R.drawable.img_cosmos_22
                Blockchain.Cronos -> R.drawable.img_cronos_22
                Blockchain.Cyber, Blockchain.CyberTestnet -> R.drawable.img_cyber_22
                Blockchain.Dash -> R.drawable.img_dash_22
                Blockchain.Decimal, Blockchain.DecimalTestnet -> R.drawable.img_decimal_22
                Blockchain.Dischain -> R.drawable.img_dischain_22
                Blockchain.Dogecoin -> R.drawable.img_dogecoin_22
                Blockchain.Ducatus -> R.drawable.img_ducatus_22
                Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet,
                Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet,
                -> R.drawable.img_energy_web_22
                Blockchain.Ethereum, Blockchain.EthereumTestnet -> R.drawable.img_eth_22
                Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> R.drawable.img_eth_classic_22
                Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> R.drawable.img_eth_pow_22
                Blockchain.Fact0rn -> R.drawable.img_fact0rn_22
                Blockchain.Fantom, Blockchain.FantomTestnet -> R.drawable.img_fantom_22
                Blockchain.Filecoin -> R.drawable.img_filecoin_22
                Blockchain.Flare, Blockchain.FlareTestnet -> R.drawable.img_flare_22
                Blockchain.Gnosis -> R.drawable.img_gnosis_22
                Blockchain.Hedera, Blockchain.HederaTestnet -> R.drawable.img_hedera_22
                Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet -> R.drawable.img_hyperliquid_22
                Blockchain.InternetComputer -> R.drawable.img_icp_22
                Blockchain.Joystream -> R.drawable.img_joystream_22
                Blockchain.Kaspa, Blockchain.KaspaTestnet -> R.drawable.img_kaspa_22
                Blockchain.Kava, Blockchain.KavaTestnet -> R.drawable.img_kava_22
                Blockchain.Koinos, Blockchain.KoinosTestnet -> R.drawable.img_koinos_22
                Blockchain.Kusama -> R.drawable.img_kusama_22
                Blockchain.Linea, Blockchain.LineaTestnet -> R.drawable.img_linea_22
                Blockchain.Litecoin -> R.drawable.img_litecoin_22
                Blockchain.Manta, Blockchain.MantaTestnet -> R.drawable.img_manta_22
                Blockchain.Mantle, Blockchain.MantleTestnet -> R.drawable.img_mantle_22
                Blockchain.Monad, Blockchain.MonadTestnet -> R.drawable.img_monad_22
                Blockchain.Moonbeam, Blockchain.MoonbeamTestnet -> R.drawable.img_moonbeam_22
                Blockchain.Moonriver, Blockchain.MoonriverTestnet -> R.drawable.img_moonriver_22
                Blockchain.Near, Blockchain.NearTestnet -> R.drawable.img_near_22
                Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet -> R.drawable.img_octaspace_22
                Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet -> R.drawable.img_odyssey_chain_22
                Blockchain.Optimism, Blockchain.OptimismTestnet -> R.drawable.img_optimism_22
                Blockchain.Pepecoin, Blockchain.PepecoinTestnet -> R.drawable.img_pepecoin_22
                Blockchain.Plasma, Blockchain.PlasmaTestnet -> R.drawable.img_plasma_22
                Blockchain.Playa3ull -> R.drawable.img_playa3ull_22
                Blockchain.Polkadot, Blockchain.PolkadotTestnet -> R.drawable.img_polkadot_22
                Blockchain.Polygon, Blockchain.PolygonTestnet -> R.drawable.img_polygon_22
                Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet -> R.drawable.img_polygon_22
                Blockchain.PulseChain, Blockchain.PulseChainTestnet -> R.drawable.img_pls_22
                Blockchain.Quai, Blockchain.QuaiTestnet -> R.drawable.img_quai_22
                Blockchain.RSK -> R.drawable.img_rsk_22
                Blockchain.Radiant -> R.drawable.img_radiant_22
                Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> R.drawable.img_ravencoin_22
                Blockchain.Scroll, Blockchain.ScrollTestnet -> R.drawable.img_scroll_22
                Blockchain.Sei, Blockchain.SeiTestnet,
                Blockchain.SeiEvm, Blockchain.SeiEvmTestnet,
                -> R.drawable.img_sei_22
                Blockchain.Shibarium, Blockchain.ShibariumTestnet -> R.drawable.img_shibarium_22
                Blockchain.Solana, Blockchain.SolanaTestnet -> R.drawable.img_solana_22
                Blockchain.Sonic, Blockchain.SonicTestnet -> R.drawable.img_sonic_22
                Blockchain.Stellar, Blockchain.StellarTestnet -> R.drawable.img_stellar_22
                Blockchain.Sui, Blockchain.SuiTestnet -> R.drawable.img_sui_22
                Blockchain.TON, Blockchain.TONTestnet -> R.drawable.img_ton_22
                Blockchain.Taraxa, Blockchain.TaraxaTestnet -> R.drawable.img_taraxa_22
                Blockchain.Telos, Blockchain.TelosTestnet -> R.drawable.img_telos_22
                Blockchain.TerraV1 -> R.drawable.img_terra_22
                Blockchain.TerraV2 -> R.drawable.img_terra2_22
                Blockchain.Tezos -> R.drawable.img_tezos_22
                Blockchain.Tron, Blockchain.TronTestnet -> R.drawable.img_tron_22
                Blockchain.VanarChain, Blockchain.VanarChainTestnet -> R.drawable.img_vanar_22
                Blockchain.VeChain, Blockchain.VeChainTestnet -> R.drawable.img_vechain_22
                Blockchain.XDC, Blockchain.XDCTestnet -> R.drawable.img_xdc_22
                Blockchain.XRP -> R.drawable.img_xrp_22
                Blockchain.Xodex -> R.drawable.img_xodex_22
                Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet -> R.drawable.img_zklink_22
                Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> R.drawable.img_zksync_22
                Blockchain.Nexa, Blockchain.NexaTestnet, Blockchain.Unknown -> R.drawable.ic_alert_24
            }
            TestModel(blockchain, expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetGreyedOutIconRes {

        @ParameterizedTest
        @ProvideTestModels
        fun returnsCorrectDrawable(model: TestModel) {
            assertThat(getGreyedOutIconRes(model.input)).isEqualTo(model.expected)
        }

        @Test
        fun returnsAlertDrawableForUnknownId() {
            assertThat(getGreyedOutIconRes(Blockchain.Unknown)).isEqualTo(R.drawable.ic_alert_24)
        }

        @Suppress("CyclomaticComplexMethod", "LongMethod")
        private fun provideTestModels() = Blockchain.entries.map { blockchain ->
            val expected = when (blockchain) {
                Blockchain.Alephium, Blockchain.AlephiumTestnet -> R.drawable.ic_alephium_22
                Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> R.drawable.ic_azero_22
                Blockchain.Algorand, Blockchain.AlgorandTestnet -> R.drawable.ic_algorand_22
                Blockchain.ApeChain, Blockchain.ApeChainTestnet -> R.drawable.ic_apecoin_22
                Blockchain.Aptos, Blockchain.AptosTestnet -> R.drawable.ic_aptos_22
                Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> R.drawable.ic_arbitrum_22
                Blockchain.ArbitrumNova -> R.drawable.ic_arbitrum_nova_22
                Blockchain.Areon, Blockchain.AreonTestnet -> R.drawable.ic_areon_22
                Blockchain.Aurora, Blockchain.AuroraTestnet -> R.drawable.ic_aurora_22
                Blockchain.Avalanche, Blockchain.AvalancheTestnet -> R.drawable.ic_avalanche_22
                Blockchain.BSC, Blockchain.BSCTestnet,
                Blockchain.Binance, Blockchain.BinanceTestnet,
                -> R.drawable.ic_bsc_16
                Blockchain.Base, Blockchain.BaseTestnet -> R.drawable.ic_base_22
                Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> R.drawable.ic_bitcoin_16
                Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> R.drawable.ic_bitcoin_cash_16
                Blockchain.Bitrock, Blockchain.BitrockTestnet -> R.drawable.ic_bitrock_22
                Blockchain.Bittensor -> R.drawable.ic_bittensor_22
                Blockchain.Blast, Blockchain.BlastTestnet -> R.drawable.ic_blast_22
                Blockchain.Canxium -> R.drawable.ic_canxium_22
                Blockchain.Cardano -> R.drawable.ic_cardano_16
                Blockchain.Casper, Blockchain.CasperTestnet -> R.drawable.ic_casper_22
                Blockchain.Chia, Blockchain.ChiaTestnet -> R.drawable.ic_chia_22
                Blockchain.Chiliz, Blockchain.ChilizTestnet -> R.drawable.ic_chiliz_22
                Blockchain.Clore -> R.drawable.ic_clore_22
                Blockchain.Core, Blockchain.CoreTestnet -> R.drawable.ic_core_22
                Blockchain.Cosmos, Blockchain.CosmosTestnet -> R.drawable.ic_cosmos_22
                Blockchain.Cronos -> R.drawable.ic_cronos_22
                Blockchain.Cyber, Blockchain.CyberTestnet -> R.drawable.ic_cyber_22
                Blockchain.Dash -> R.drawable.ic_dash_22
                Blockchain.Decimal, Blockchain.DecimalTestnet -> R.drawable.ic_decimal_22
                Blockchain.Dischain -> R.drawable.ic_dischain_22
                Blockchain.Dogecoin -> R.drawable.ic_dogecoin_16
                Blockchain.Ducatus -> R.drawable.ic_ducatus_22
                Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet,
                Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet,
                -> R.drawable.ic_energy_web_22
                Blockchain.Ethereum, Blockchain.EthereumTestnet -> R.drawable.ic_eth_16
                Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> R.drawable.ic_eth_16
                Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> R.drawable.ic_ethereumpow_22
                Blockchain.Fact0rn -> R.drawable.ic_fact0rn_22
                Blockchain.Fantom, Blockchain.FantomTestnet -> R.drawable.ic_fantom_22
                Blockchain.Filecoin -> R.drawable.ic_filecoin_22
                Blockchain.Flare, Blockchain.FlareTestnet -> R.drawable.ic_flare_22
                Blockchain.Gnosis -> R.drawable.ic_gnosis_22
                Blockchain.Hedera, Blockchain.HederaTestnet -> R.drawable.ic_hedera_22
                Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet -> R.drawable.ic_hyperliquid_22
                Blockchain.InternetComputer -> R.drawable.ic_icp_22
                Blockchain.Joystream -> R.drawable.ic_joystream_22
                Blockchain.Kaspa, Blockchain.KaspaTestnet -> R.drawable.ic_kaspa_22
                Blockchain.Kava, Blockchain.KavaTestnet -> R.drawable.ic_kava_22
                Blockchain.Koinos, Blockchain.KoinosTestnet -> R.drawable.ic_koinos_22
                Blockchain.Kusama -> R.drawable.ic_kusama_16
                Blockchain.Linea, Blockchain.LineaTestnet -> R.drawable.ic_linea_22
                Blockchain.Litecoin -> R.drawable.ic_litecoin_22
                Blockchain.Manta, Blockchain.MantaTestnet -> R.drawable.ic_manta_22
                Blockchain.Mantle, Blockchain.MantleTestnet -> R.drawable.ic_mantle_22
                Blockchain.Monad, Blockchain.MonadTestnet -> R.drawable.ic_monad_22
                Blockchain.Moonbeam, Blockchain.MoonbeamTestnet -> R.drawable.ic_moonbeam_22
                Blockchain.Moonriver, Blockchain.MoonriverTestnet -> R.drawable.ic_moonriver_22
                Blockchain.Near, Blockchain.NearTestnet -> R.drawable.ic_near_22
                Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet -> R.drawable.ic_octaspace_22
                Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet -> R.drawable.ic_odyssey_chain_22
                Blockchain.Optimism, Blockchain.OptimismTestnet -> R.drawable.ic_optimism_22
                Blockchain.Pepecoin, Blockchain.PepecoinTestnet -> R.drawable.ic_pepecoin_22
                Blockchain.Plasma, Blockchain.PlasmaTestnet -> R.drawable.ic_plasma_22
                Blockchain.Playa3ull -> R.drawable.ic_playa3ull_22
                Blockchain.Polkadot, Blockchain.PolkadotTestnet -> R.drawable.ic_polkadot_16
                Blockchain.Polygon, Blockchain.PolygonTestnet -> R.drawable.ic_polygon_22
                Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet -> R.drawable.ic_polygon_22
                Blockchain.PulseChain, Blockchain.PulseChainTestnet -> R.drawable.ic_pls_22
                Blockchain.Quai, Blockchain.QuaiTestnet -> R.drawable.ic_quai_22
                Blockchain.RSK -> R.drawable.ic_rsk_16
                Blockchain.Radiant -> R.drawable.ic_radiant_22
                Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> R.drawable.ic_ravencoin_22
                Blockchain.Scroll, Blockchain.ScrollTestnet -> R.drawable.ic_scroll_22
                Blockchain.Sei, Blockchain.SeiTestnet,
                Blockchain.SeiEvm, Blockchain.SeiEvmTestnet,
                -> R.drawable.ic_sei_22
                Blockchain.Shibarium, Blockchain.ShibariumTestnet -> R.drawable.ic_shibarium_22
                Blockchain.Solana, Blockchain.SolanaTestnet -> R.drawable.ic_solana_16
                Blockchain.Sonic, Blockchain.SonicTestnet -> R.drawable.ic_sonic_22
                Blockchain.Stellar, Blockchain.StellarTestnet -> R.drawable.ic_stellar_16
                Blockchain.Sui, Blockchain.SuiTestnet -> R.drawable.ic_sui_22
                Blockchain.TON, Blockchain.TONTestnet -> R.drawable.ic_ton_22
                Blockchain.Taraxa, Blockchain.TaraxaTestnet -> R.drawable.ic_taraxa_22
                Blockchain.Telos, Blockchain.TelosTestnet -> R.drawable.ic_telos_22
                Blockchain.TerraV1 -> R.drawable.ic_terra_22
                Blockchain.TerraV2 -> R.drawable.ic_terra2_22
                Blockchain.Tezos -> R.drawable.ic_tezos_16
                Blockchain.Tron, Blockchain.TronTestnet -> R.drawable.ic_tron_22
                Blockchain.VanarChain, Blockchain.VanarChainTestnet -> R.drawable.ic_vanar_22
                Blockchain.VeChain, Blockchain.VeChainTestnet -> R.drawable.ic_vechain_22
                Blockchain.XDC, Blockchain.XDCTestnet -> R.drawable.ic_xdc_22
                Blockchain.XRP -> R.drawable.ic_xrp_22
                Blockchain.Xodex -> R.drawable.ic_xodex_22
                Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet -> R.drawable.ic_zklink_22
                Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> R.drawable.ic_zksync_22
                Blockchain.Nexa, Blockchain.NexaTestnet, Blockchain.Unknown -> R.drawable.ic_alert_24
            }
            TestModel(blockchain, expected)
        }
    }

    data class TestModel(val input: Blockchain, val expected: Int)
}