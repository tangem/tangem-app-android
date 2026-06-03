package com.tangem.common.ui.extensions

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.R

/**
 * Holds icon resources for a single [Blockchain].
 *
 * @property active     drawable for the active state
 * @property greyedOut  drawable for the disabled / greyed-out state
 */
private data class IconSet(
    @DrawableRes val active: Int,
    @DrawableRes val greyedOut: Int,
)

/**
 * Returns the [IconSet] for the given [blockchain], or `null` if icons are not yet available.
 *
 * The `when` is exhaustive — adding a new [Blockchain] entry in the SDK without handling it here
 * will cause a compile-time error.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun iconSetOf(blockchain: Blockchain): IconSet? = when (blockchain) {
    Blockchain.Alephium,
    Blockchain.AlephiumTestnet,
    -> IconSet(active = R.drawable.img_alephium_22, greyedOut = R.drawable.ic_alephium_22)
    Blockchain.AlephZero,
    Blockchain.AlephZeroTestnet,
    -> IconSet(active = R.drawable.img_azero_22, greyedOut = R.drawable.ic_azero_22)
    Blockchain.Algorand,
    Blockchain.AlgorandTestnet,
    -> IconSet(active = R.drawable.img_algorand_22, greyedOut = R.drawable.ic_algorand_22)
    Blockchain.ApeChain,
    Blockchain.ApeChainTestnet,
    -> IconSet(active = R.drawable.img_apecoin_22, greyedOut = R.drawable.ic_apecoin_22)
    Blockchain.Aptos,
    Blockchain.AptosTestnet,
    -> IconSet(active = R.drawable.img_aptos_22, greyedOut = R.drawable.ic_aptos_22)
    Blockchain.Arbitrum,
    Blockchain.ArbitrumTestnet,
    -> IconSet(active = R.drawable.img_arbitrum_22, greyedOut = R.drawable.ic_arbitrum_22)
    Blockchain.ArbitrumNova,
    -> IconSet(active = R.drawable.img_arbitrum_nova_22, greyedOut = R.drawable.ic_arbitrum_nova_22)
    Blockchain.Areon,
    Blockchain.AreonTestnet,
    -> IconSet(active = R.drawable.img_areon_22, greyedOut = R.drawable.ic_areon_22)
    Blockchain.Aurora,
    Blockchain.AuroraTestnet,
    -> IconSet(active = R.drawable.img_aurora_22, greyedOut = R.drawable.ic_aurora_22)
    Blockchain.Avalanche,
    Blockchain.AvalancheTestnet,
    -> IconSet(active = R.drawable.img_avalanche_22, greyedOut = R.drawable.ic_avalanche_22)
    Blockchain.BSC,
    Blockchain.BSCTestnet,
    Blockchain.Binance,
    Blockchain.BinanceTestnet,
    -> IconSet(active = R.drawable.img_bsc_22, greyedOut = R.drawable.ic_bsc_16)
    Blockchain.Base,
    Blockchain.BaseTestnet,
    -> IconSet(active = R.drawable.img_base_22, greyedOut = R.drawable.ic_base_22)
    Blockchain.Bitcoin,
    Blockchain.BitcoinTestnet,
    -> IconSet(active = R.drawable.img_btc_22, greyedOut = R.drawable.ic_bitcoin_16)
    Blockchain.BitcoinCash,
    Blockchain.BitcoinCashTestnet,
    -> IconSet(active = R.drawable.img_btc_cash_22, greyedOut = R.drawable.ic_bitcoin_cash_16)
    Blockchain.Bitrock,
    Blockchain.BitrockTestnet,
    -> IconSet(active = R.drawable.img_bitrock_22, greyedOut = R.drawable.ic_bitrock_22)
    Blockchain.Bittensor,
    -> IconSet(active = R.drawable.img_bittensor_22, greyedOut = R.drawable.ic_bittensor_22)
    Blockchain.Blast,
    Blockchain.BlastTestnet,
    -> IconSet(active = R.drawable.img_blast_22, greyedOut = R.drawable.ic_blast_22)
    Blockchain.Canxium,
    -> IconSet(active = R.drawable.img_canxium_22, greyedOut = R.drawable.ic_canxium_22)
    Blockchain.Cardano,
    -> IconSet(active = R.drawable.img_cardano_22, greyedOut = R.drawable.ic_cardano_16)
    Blockchain.Casper,
    Blockchain.CasperTestnet,
    -> IconSet(active = R.drawable.img_casper_22, greyedOut = R.drawable.ic_casper_22)
    Blockchain.Chia,
    Blockchain.ChiaTestnet,
    -> IconSet(active = R.drawable.img_chia_22, greyedOut = R.drawable.ic_chia_22)
    Blockchain.Chiliz,
    Blockchain.ChilizTestnet,
    -> IconSet(active = R.drawable.img_chiliz_22, greyedOut = R.drawable.ic_chiliz_22)
    Blockchain.Clore,
    -> IconSet(active = R.drawable.img_clore_22, greyedOut = R.drawable.ic_clore_22)
    Blockchain.Core,
    Blockchain.CoreTestnet,
    -> IconSet(active = R.drawable.img_core_22, greyedOut = R.drawable.ic_core_22)
    Blockchain.Cosmos,
    Blockchain.CosmosTestnet,
    -> IconSet(active = R.drawable.img_cosmos_22, greyedOut = R.drawable.ic_cosmos_22)
    Blockchain.Cronos,
    -> IconSet(active = R.drawable.img_cronos_22, greyedOut = R.drawable.ic_cronos_22)
    Blockchain.Cyber,
    Blockchain.CyberTestnet,
    -> IconSet(active = R.drawable.img_cyber_22, greyedOut = R.drawable.ic_cyber_22)
    Blockchain.Dash,
    -> IconSet(active = R.drawable.img_dash_22, greyedOut = R.drawable.ic_dash_22)
    Blockchain.Decimal,
    Blockchain.DecimalTestnet,
    -> IconSet(active = R.drawable.img_decimal_22, greyedOut = R.drawable.ic_decimal_22)
    Blockchain.Dischain,
    -> IconSet(active = R.drawable.img_dischain_22, greyedOut = R.drawable.ic_dischain_22)
    Blockchain.Dogecoin,
    -> IconSet(active = R.drawable.img_dogecoin_22, greyedOut = R.drawable.ic_dogecoin_16)
    Blockchain.Ducatus,
    -> IconSet(active = R.drawable.img_ducatus_22, greyedOut = R.drawable.ic_ducatus_22)
    Blockchain.EnergyWebChain,
    Blockchain.EnergyWebChainTestnet,
    Blockchain.EnergyWebX,
    Blockchain.EnergyWebXTestnet,
    -> IconSet(active = R.drawable.img_energy_web_22, greyedOut = R.drawable.ic_energy_web_22)
    Blockchain.Ethereum,
    Blockchain.EthereumTestnet,
    -> IconSet(active = R.drawable.img_eth_22, greyedOut = R.drawable.ic_eth_16)
    Blockchain.EthereumClassic,
    Blockchain.EthereumClassicTestnet,
    -> IconSet(active = R.drawable.img_eth_classic_22, greyedOut = R.drawable.ic_eth_16)
    Blockchain.EthereumPow,
    Blockchain.EthereumPowTestnet,
    -> IconSet(active = R.drawable.img_eth_pow_22, greyedOut = R.drawable.ic_ethereumpow_22)
    Blockchain.Fact0rn,
    -> IconSet(active = R.drawable.img_fact0rn_22, greyedOut = R.drawable.ic_fact0rn_22)
    Blockchain.Fantom,
    Blockchain.FantomTestnet,
    -> IconSet(active = R.drawable.img_fantom_22, greyedOut = R.drawable.ic_fantom_22)
    Blockchain.Filecoin,
    -> IconSet(active = R.drawable.img_filecoin_22, greyedOut = R.drawable.ic_filecoin_22)
    Blockchain.Flare,
    Blockchain.FlareTestnet,
    -> IconSet(active = R.drawable.img_flare_22, greyedOut = R.drawable.ic_flare_22)
    Blockchain.Gnosis,
    -> IconSet(active = R.drawable.img_gnosis_22, greyedOut = R.drawable.ic_gnosis_22)
    Blockchain.Hedera,
    Blockchain.HederaTestnet,
    -> IconSet(active = R.drawable.img_hedera_22, greyedOut = R.drawable.ic_hedera_22)
    Blockchain.Hyperliquid,
    Blockchain.HyperliquidTestnet,
    -> IconSet(active = R.drawable.img_hyperliquid_22, greyedOut = R.drawable.ic_hyperliquid_22)
    Blockchain.InternetComputer,
    -> IconSet(active = R.drawable.img_icp_22, greyedOut = R.drawable.ic_icp_22)
    Blockchain.Joystream,
    -> IconSet(active = R.drawable.img_joystream_22, greyedOut = R.drawable.ic_joystream_22)
    Blockchain.Kaspa,
    Blockchain.KaspaTestnet,
    -> IconSet(active = R.drawable.img_kaspa_22, greyedOut = R.drawable.ic_kaspa_22)
    Blockchain.Kava,
    Blockchain.KavaTestnet,
    -> IconSet(active = R.drawable.img_kava_22, greyedOut = R.drawable.ic_kava_22)
    Blockchain.Koinos,
    Blockchain.KoinosTestnet,
    -> IconSet(active = R.drawable.img_koinos_22, greyedOut = R.drawable.ic_koinos_22)
    Blockchain.Kusama,
    -> IconSet(active = R.drawable.img_kusama_22, greyedOut = R.drawable.ic_kusama_16)
    Blockchain.Linea,
    Blockchain.LineaTestnet,
    -> IconSet(active = R.drawable.img_linea_22, greyedOut = R.drawable.ic_linea_22)
    Blockchain.Litecoin,
    -> IconSet(active = R.drawable.img_litecoin_22, greyedOut = R.drawable.ic_litecoin_22)
    Blockchain.Manta,
    Blockchain.MantaTestnet,
    -> IconSet(active = R.drawable.img_manta_22, greyedOut = R.drawable.ic_manta_22)
    Blockchain.Mantle,
    Blockchain.MantleTestnet,
    -> IconSet(active = R.drawable.img_mantle_22, greyedOut = R.drawable.ic_mantle_22)
    Blockchain.Monad,
    Blockchain.MonadTestnet,
    -> IconSet(active = R.drawable.img_monad_22, greyedOut = R.drawable.ic_monad_22)
    Blockchain.Moonbeam,
    Blockchain.MoonbeamTestnet,
    -> IconSet(active = R.drawable.img_moonbeam_22, greyedOut = R.drawable.ic_moonbeam_22)
    Blockchain.Moonriver,
    Blockchain.MoonriverTestnet,
    -> IconSet(active = R.drawable.img_moonriver_22, greyedOut = R.drawable.ic_moonriver_22)
    Blockchain.Near,
    Blockchain.NearTestnet,
    -> IconSet(active = R.drawable.img_near_22, greyedOut = R.drawable.ic_near_22)
    Blockchain.OctaSpace,
    Blockchain.OctaSpaceTestnet,
    -> IconSet(active = R.drawable.img_octaspace_22, greyedOut = R.drawable.ic_octaspace_22)
    Blockchain.OdysseyChain,
    Blockchain.OdysseyChainTestnet,
    -> IconSet(active = R.drawable.img_odyssey_chain_22, greyedOut = R.drawable.ic_odyssey_chain_22)
    Blockchain.Optimism,
    Blockchain.OptimismTestnet,
    -> IconSet(active = R.drawable.img_optimism_22, greyedOut = R.drawable.ic_optimism_22)
    Blockchain.Pepecoin,
    Blockchain.PepecoinTestnet,
    -> IconSet(active = R.drawable.img_pepecoin_22, greyedOut = R.drawable.ic_pepecoin_22)
    Blockchain.Plasma,
    Blockchain.PlasmaTestnet,
    -> IconSet(active = R.drawable.img_plasma_22, greyedOut = R.drawable.ic_plasma_22)
    Blockchain.Adi,
    Blockchain.AdiTestnet,
    -> IconSet(active = R.drawable.img_adi_22, greyedOut = R.drawable.ic_adi_22)
    Blockchain.Playa3ull,
    -> IconSet(active = R.drawable.img_playa3ull_22, greyedOut = R.drawable.ic_playa3ull_22)
    Blockchain.Polkadot,
    Blockchain.PolkadotTestnet,
    -> IconSet(active = R.drawable.img_polkadot_22, greyedOut = R.drawable.ic_polkadot_16)
    Blockchain.Polygon,
    Blockchain.PolygonTestnet,
    -> IconSet(active = R.drawable.img_polygon_22, greyedOut = R.drawable.ic_polygon_22)
    Blockchain.PolygonZkEVM,
    Blockchain.PolygonZkEVMTestnet,
    -> IconSet(active = R.drawable.img_polygon_22, greyedOut = R.drawable.ic_polygon_22)
    Blockchain.PulseChain,
    Blockchain.PulseChainTestnet,
    -> IconSet(active = R.drawable.img_pls_22, greyedOut = R.drawable.ic_pls_22)
    Blockchain.Quai,
    Blockchain.QuaiTestnet,
    -> IconSet(active = R.drawable.img_quai_22, greyedOut = R.drawable.ic_quai_22)
    Blockchain.RSK,
    -> IconSet(active = R.drawable.img_rsk_22, greyedOut = R.drawable.ic_rsk_16)
    Blockchain.Radiant,
    -> IconSet(active = R.drawable.img_radiant_22, greyedOut = R.drawable.ic_radiant_22)
    Blockchain.Ravencoin,
    Blockchain.RavencoinTestnet,
    -> IconSet(active = R.drawable.img_ravencoin_22, greyedOut = R.drawable.ic_ravencoin_22)
    Blockchain.Scroll,
    Blockchain.ScrollTestnet,
    -> IconSet(active = R.drawable.img_scroll_22, greyedOut = R.drawable.ic_scroll_22)
    Blockchain.Sei,
    Blockchain.SeiTestnet,
    Blockchain.SeiEvm,
    Blockchain.SeiEvmTestnet,
    -> IconSet(active = R.drawable.img_sei_22, greyedOut = R.drawable.ic_sei_22)
    Blockchain.Shibarium,
    Blockchain.ShibariumTestnet,
    -> IconSet(active = R.drawable.img_shibarium_22, greyedOut = R.drawable.ic_shibarium_22)
    Blockchain.Solana,
    Blockchain.SolanaTestnet,
    -> IconSet(active = R.drawable.img_solana_22, greyedOut = R.drawable.ic_solana_16)
    Blockchain.Sonic,
    Blockchain.SonicTestnet,
    -> IconSet(active = R.drawable.img_sonic_22, greyedOut = R.drawable.ic_sonic_22)
    Blockchain.Stellar,
    Blockchain.StellarTestnet,
    -> IconSet(active = R.drawable.img_stellar_22, greyedOut = R.drawable.ic_stellar_16)
    Blockchain.Sui,
    Blockchain.SuiTestnet,
    -> IconSet(active = R.drawable.img_sui_22, greyedOut = R.drawable.ic_sui_22)
    Blockchain.TON,
    Blockchain.TONTestnet,
    -> IconSet(active = R.drawable.img_ton_22, greyedOut = R.drawable.ic_ton_22)
    Blockchain.Taraxa,
    Blockchain.TaraxaTestnet,
    -> IconSet(active = R.drawable.img_taraxa_22, greyedOut = R.drawable.ic_taraxa_22)
    Blockchain.Telos,
    Blockchain.TelosTestnet,
    -> IconSet(active = R.drawable.img_telos_22, greyedOut = R.drawable.ic_telos_22)
    Blockchain.TerraV1,
    -> IconSet(active = R.drawable.img_terra_22, greyedOut = R.drawable.ic_terra_22)
    Blockchain.TerraV2,
    -> IconSet(active = R.drawable.img_terra2_22, greyedOut = R.drawable.ic_terra2_22)
    Blockchain.Tezos,
    -> IconSet(active = R.drawable.img_tezos_22, greyedOut = R.drawable.ic_tezos_16)
    Blockchain.Tron,
    Blockchain.TronTestnet,
    -> IconSet(active = R.drawable.img_tron_22, greyedOut = R.drawable.ic_tron_22)
    Blockchain.VanarChain,
    Blockchain.VanarChainTestnet,
    -> IconSet(active = R.drawable.img_vanar_22, greyedOut = R.drawable.ic_vanar_22)
    Blockchain.VeChain,
    Blockchain.VeChainTestnet,
    -> IconSet(active = R.drawable.img_vechain_22, greyedOut = R.drawable.ic_vechain_22)
    Blockchain.XDC,
    Blockchain.XDCTestnet,
    -> IconSet(active = R.drawable.img_xdc_22, greyedOut = R.drawable.ic_xdc_22)
    Blockchain.XRP,
    -> IconSet(active = R.drawable.img_xrp_22, greyedOut = R.drawable.ic_xrp_22)
    Blockchain.Xodex,
    -> IconSet(active = R.drawable.img_xodex_22, greyedOut = R.drawable.ic_xodex_22)
    Blockchain.ZkLinkNova,
    Blockchain.ZkLinkNovaTestnet,
    -> IconSet(active = R.drawable.img_zklink_22, greyedOut = R.drawable.ic_zklink_22)
    Blockchain.ZkSyncEra,
    Blockchain.ZkSyncEraTestnet,
    -> IconSet(active = R.drawable.img_zksync_22, greyedOut = R.drawable.ic_zksync_22)
    Blockchain.Nexa,
    Blockchain.NexaTestnet,
    Blockchain.Unknown,
    -> null
}

private val ICONS_BY_BLOCKCHAIN: Map<Blockchain, IconSet> = Blockchain.entries
    .mapNotNull { blockchain -> iconSetOf(blockchain)?.let { blockchain to it } }
    .toMap()

/**
 * Returns the active (colored) icon drawable resource for the given [blockchain].
 *
 * @param blockchain the blockchain to look up
 * @param fallback   drawable returned when [blockchain] has no icon defined
 */
@DrawableRes
fun getActiveIconRes(blockchain: Blockchain, @DrawableRes fallback: Int = R.drawable.ic_alert_24): Int {
    return ICONS_BY_BLOCKCHAIN[blockchain]?.active ?: fallback
}

/**
 * Returns the greyed-out (disabled) icon drawable resource for the given [blockchain].
 *
 * @param blockchain the blockchain to look up
 * @param fallback   drawable returned when [blockchain] has no icon defined
 */
@DrawableRes
fun getGreyedOutIconRes(blockchain: Blockchain, @DrawableRes fallback: Int = R.drawable.ic_alert_24): Int {
    return ICONS_BY_BLOCKCHAIN[blockchain]?.greyedOut ?: fallback
}