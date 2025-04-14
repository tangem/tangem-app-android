package com.tangem.core.ui.extensions

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R

@Suppress("ComplexMethod", "LongMethod")
@DrawableRes
fun getActiveIconRes(blockchainId: String): Int {
    return when (blockchainId) {
        "ARBITRUM-ONE", "ARBITRUM/test" -> R.drawable.img_arbitrum_22
        "BTC", "BTC/test" -> R.drawable.img_btc_22
        "BCH" -> R.drawable.img_btc_cash_22
        "LTC" -> R.drawable.img_litecoin_22
        "ETH", "ETH/test" -> R.drawable.img_eth_22
        "ETC", "ETC/test" -> R.drawable.img_eth_classic_22
        "RSK" -> R.drawable.img_rsk_22
        "CARDANO", "CARDANO-S" -> R.drawable.img_cardano_22
        "XTZ" -> R.drawable.img_tezos_22
        "XRP" -> R.drawable.img_xrp_22
        "XLM", "XLM/test" -> R.drawable.img_stellar_22
        "AVALANCHE", "AVALANCHE/test" -> R.drawable.img_avalanche_22
        "POLYGON", "POLYGON/test" -> R.drawable.img_polygon_22
        "SOLANA", "SOLANA/test" -> R.drawable.img_solana_22
        "FTM", "FTM/test" -> R.drawable.img_fantom_22
        "BSC", "BSC/test", "BINANCE", "BINANCE/test" -> R.drawable.img_bsc_22
        "DOGE" -> R.drawable.img_dogecoin_22
        "TRON", "TRON/test" -> R.drawable.img_tron_22
        "GNO" -> R.drawable.img_gnosis_22
        "ETH-Pow", "ETH-Pow/test" -> R.drawable.img_eth_pow_22
        "ETH-Fair", "dischain" -> R.drawable.img_dischain_22
        "Polkadot", "Polkadot/test" -> R.drawable.img_polkadot_22
        "Kusama" -> R.drawable.img_kusama_22
        "OPTIMISM", "OPTIMISM/test" -> R.drawable.img_optimism_22
        "DASH" -> R.drawable.img_dash_22
        "KAS", "KAS/test" -> R.drawable.img_kaspa_22
        "The-Open-Network", "The-Open-Network/test" -> R.drawable.img_ton_22
        "KAVA", "KAVA/test" -> R.drawable.img_kava_22
        "ravencoin", "ravencoin/test" -> R.drawable.img_ravencoin_22
        "cosmos", "cosmos/test" -> R.drawable.img_cosmos_22
        "terra", "terra-luna" -> R.drawable.img_terra_22
        "terra-2", "terra-luna-2" -> R.drawable.img_terra2_22
        "cronos" -> R.drawable.img_cronos_22
        "TELOS", "TELOS/test" -> R.drawable.img_telos_22
        "aleph-zero", "aleph-zero/test" -> R.drawable.img_azero_22
        "octaspace", "octaspace/test" -> R.drawable.img_octaspace_22
        "chia", "chia/test" -> R.drawable.img_chia_22
        "NEAR", "NEAR/test" -> R.drawable.img_near_22
        "decimal", "decimal/test" -> R.drawable.img_decimal_22
        "xdc", "xdc/test" -> R.drawable.img_xdc_22
        "vechain", "vechain/test" -> R.drawable.img_vechain_22
        "aptos", "aptos/test" -> R.drawable.img_aptos_22
        "shibarium", "shibarium/test" -> R.drawable.img_shibarium_22
        "algorand", "algorand/test" -> R.drawable.img_algorand_22
        "hedera", "hedera/test" -> R.drawable.img_hedera_22
        "playa3ull" -> R.drawable.img_playa3ull_22
        "DUC" -> R.drawable.img_ducatus_22
        "aurora", "aurora/test" -> R.drawable.img_aurora_22
        "areon", "areon/test" -> R.drawable.img_areon_22
        "pls", "pls/test" -> R.drawable.img_pls_22
        "zkSyncEra", "zkSyncEra/test" -> R.drawable.img_zksync_22
        "moonbeam", "moonbeam/test" -> R.drawable.img_moonbeam_22
        "manta-pacific", "manta/test" -> R.drawable.img_manta_22
        "polygonZkEVM", "polygonZkEVM/test" -> R.drawable.img_polygon_22
        "moonriver", "moonriver/test" -> R.drawable.img_moonriver_22
        "mantle", "mantle/test" -> R.drawable.img_mantle_22
        "flare", "flare/test" -> R.drawable.img_flare_22
        "taraxa", "taraxa/test" -> R.drawable.img_taraxa_22
        "radiant" -> R.drawable.img_radiant_22
        "base" -> R.drawable.img_base_22
        "joystream" -> R.drawable.img_joystream_22
        "koinos", "koinos/test" -> R.drawable.img_koinos_22
        "bittensor" -> R.drawable.img_bittensor_22
        "blast", "blast/test" -> R.drawable.img_blast_22
        "filecoin" -> R.drawable.img_filecoin_22
        "cyber", "cyber/test" -> R.drawable.img_cyber_22
        "sei", "sei/test" -> R.drawable.img_sei_22
        "internet-computer" -> R.drawable.img_icp_22
        "sui", "sui/test" -> R.drawable.img_sui_22
        "energy-web-chain", "energy-web-chain/test" -> R.drawable.img_energy_web_22
        "energy-web-x", "energy-web-x/test" -> R.drawable.img_energy_web_22
        "core", "core/test" -> R.drawable.img_core_22
        "casper", "casper/test" -> R.drawable.img_casper_22
        "xodex" -> R.drawable.img_xodex_22
        "canxium" -> R.drawable.img_canxium_22
        "chiliz", "chiliz/test" -> R.drawable.img_chiliz_22
        "alephium", "alephium/test" -> R.drawable.img_alephium_22
        "clore-ai" -> R.drawable.img_clore_22
        "fact0rn" -> R.drawable.img_fact0rn_22
        "odyssey", "odyssey/test" -> R.drawable.img_odyssey_chain_22
        "bitrock", "bitrock/test" -> R.drawable.img_bitrock_22
        "sonic", "sonic/test" -> R.drawable.img_sonic_22
        "apechain", "apechain/test" -> R.drawable.img_apecoin_22
        "scroll", "scroll/test" -> R.drawable.ic_alert_24 // FIXME: add icon during full integration
        "zklink", "zklink/test" -> R.drawable.img_zklink_22
        "vanar-chain", "vanar-chain/test" -> R.drawable.img_vanar_22
        "pepecoin", "pepecoin/test" -> R.drawable.img_pepecoin_22
        else -> R.drawable.ic_alert_24
    }
}

@Suppress("ComplexMethod", "LongMethod")
@DrawableRes
fun getActiveIconResByCoinId(coinId: String): Int {
    return when (coinId) {
        "binancecoin" -> R.drawable.img_bsc_22
        "bitcoin" -> R.drawable.img_btc_22
        "bitcoin-cash" -> R.drawable.img_btc_cash_22
        "ethereum" -> R.drawable.img_eth_22
        "arbitrum-one" -> R.drawable.img_arbitrum_22
        "optimistic-ethereum" -> R.drawable.img_optimism_22
        "ethereum-classic" -> R.drawable.img_eth_classic_22
        "stellar" -> R.drawable.img_stellar_22
        "cardano" -> R.drawable.img_cardano_22
        "matic-network", "polygon-ecosystem-token" -> R.drawable.img_polygon_22
        "avalanche-2" -> R.drawable.img_avalanche_22
        "solana" -> R.drawable.img_solana_22
        "fantom" -> R.drawable.img_fantom_22
        "tron" -> R.drawable.img_tron_22
        "polkadot" -> R.drawable.img_polkadot_22
        "litecoin" -> R.drawable.img_litecoin_22
        "rootstock" -> R.drawable.img_rsk_22
        "tezos" -> R.drawable.img_tezos_22
        "ripple" -> R.drawable.img_xrp_22
        "dogecoin" -> R.drawable.img_dogecoin_22
        "xdai" -> R.drawable.img_gnosis_22
        "ethereum-pow-iou" -> R.drawable.img_eth_pow_22
        "ethereumfair", "dischain" -> R.drawable.img_dischain_22
        "kusama" -> R.drawable.img_kusama_22
        "dash" -> R.drawable.img_dash_22
        "kaspa", "kaspa/test" -> R.drawable.img_kaspa_22
        "ton" -> R.drawable.img_ton_22
        "kava" -> R.drawable.img_kava_22
        "ravencoin" -> R.drawable.img_ravencoin_22
        "terra" -> R.drawable.img_terra_22
        "terra-2" -> R.drawable.img_terra2_22
        "telos" -> R.drawable.img_telos_22
        "octaspace" -> R.drawable.img_octaspace_22
        "chia" -> R.drawable.img_chia_22
        "near" -> R.drawable.img_near_22
        "decimal" -> R.drawable.img_decimal_22
        "xdce-crowd-sale" -> R.drawable.img_xdc_22
        "vechain" -> R.drawable.img_vechain_22
        "aptos" -> R.drawable.img_aptos_22
        "shibarium" -> R.drawable.img_shibarium_22
        "algorand" -> R.drawable.img_algorand_22
        "hedera-hashgraph" -> R.drawable.img_hedera_22
        "playa3ull-games-2" -> R.drawable.img_playa3ull_22
        "ducatus" -> R.drawable.img_ducatus_22
        "aurora-near" -> R.drawable.img_aurora_22
        "areon" -> R.drawable.img_areon_22
        "pls" -> R.drawable.img_pls_22
        "zksync-ethereum" -> R.drawable.img_zksync_22
        "moonbeam" -> R.drawable.img_moonbeam_22
        "manta-pacific" -> R.drawable.img_manta_22
        "polygon-zkevm-ethereum" -> R.drawable.img_polygon_22
        "moonriver" -> R.drawable.img_moonriver_22
        "mantle" -> R.drawable.img_mantle_22
        "flare-networks" -> R.drawable.img_flare_22
        "taraxa" -> R.drawable.img_taraxa_22
        "radiant" -> R.drawable.img_radiant_22
        "base" -> R.drawable.img_base_22
        "joystream" -> R.drawable.img_joystream_22
        "koinos", "koinos/test" -> R.drawable.img_koinos_22
        "bittensor" -> R.drawable.img_bittensor_22
        "blast", "blast/test" -> R.drawable.img_blast_22
        "filecoin" -> R.drawable.img_filecoin_22
        "cyber", "cyber/test" -> R.drawable.img_cyber_22
        "sei", "sei/test" -> R.drawable.img_sei_22
        "internet-computer" -> R.drawable.img_icp_22
        "sui", "sui/test" -> R.drawable.img_sui_22
        "energy-web-chain", "energy-web-chain/test" -> R.drawable.img_energy_web_22
        "energy-web-x", "energy-web-x/test" -> R.drawable.img_energy_web_22
        "core", "core/test" -> R.drawable.img_core_22
        "casper-network" -> R.drawable.img_casper_22
        "xodex" -> R.drawable.img_xodex_22
        "canxium" -> R.drawable.img_canxium_22
        "chiliz", "chiliz/test" -> R.drawable.img_chiliz_22
        "alephium", "alephium/test" -> R.drawable.img_alephium_22
        "clore-ai" -> R.drawable.img_clore_22
        "fact0rn" -> R.drawable.img_fact0rn_22
        "odyssey", "odyssey/test" -> R.drawable.img_odyssey_chain_22
        "bitrock", "bitrock/test" -> R.drawable.img_bitrock_22
        "sonic", "sonic/test" -> R.drawable.img_sonic_22
        "apechain", "apechain/test" -> R.drawable.img_apecoin_22
        "scroll", "scroll/test" -> R.drawable.ic_alert_24 // FIXME: add icon during full integration
        "zklink", "zklink/test" -> R.drawable.img_zklink_22
        "vanar-chain", "vanar-chain/test" -> R.drawable.img_vanar_22
        "pepecoin-network", "pepecoin-network/test" -> R.drawable.img_pepecoin_22
        else -> R.drawable.ic_alert_24
    }
}

@Suppress("ComplexMethod", "LongMethod")
@DrawableRes
fun getGreyedOutIconRes(blockchainId: String): Int {
    return when (blockchainId) {
        "ARBITRUM-ONE", "ARBITRUM/test" -> R.drawable.ic_arbitrum_22
        "BTC", "BTC/test" -> R.drawable.ic_bitcoin_16
        "BCH" -> R.drawable.ic_bitcoin_cash_16
        "LTC" -> R.drawable.ic_litecoin_22
        "ETH", "ETH/test" -> R.drawable.ic_eth_16
        "ETC", "ETC/test" -> R.drawable.ic_eth_16
        "RSK" -> R.drawable.ic_rsk_16
        "CARDANO", "CARDANO-S" -> R.drawable.ic_cardano_16
        "XTZ" -> R.drawable.ic_tezos_16
        "XRP" -> R.drawable.ic_xrp_22
        "XLM", "XLM/test" -> R.drawable.ic_stellar_16
        "AVALANCHE", "AVALANCHE/test" -> R.drawable.ic_avalanche_22
        "POLYGON", "POLYGON/test" -> R.drawable.ic_polygon_22
        "SOLANA", "SOLANA/test" -> R.drawable.ic_solana_16
        "FTM", "FTM/test" -> R.drawable.ic_fantom_22
        "BSC", "BSC/test", "BINANCE", "BINANCE/test" -> R.drawable.ic_bsc_16
        "DOGE" -> R.drawable.ic_dogecoin_16
        "TRON", "TRON/test" -> R.drawable.ic_tron_22
        "GNO" -> R.drawable.ic_gnosis_22
        "ETH-Pow", "ETH-Pow/test" -> R.drawable.ic_ethereumpow_22
        "ETH-Fair", "dischain" -> R.drawable.ic_dischain_22
        "Polkadot", "Polkadot/test" -> R.drawable.ic_polkadot_16
        "Kusama" -> R.drawable.ic_kusama_16
        "OPTIMISM", "OPTIMISM/test" -> R.drawable.ic_optimism_22
        "DASH" -> R.drawable.ic_dash_22
        "KAS", "KAS/test" -> R.drawable.ic_kaspa_22
        "The-Open-Network", "The-Open-Network/test" -> R.drawable.ic_ton_22
        "KAVA", "KAVA/test" -> R.drawable.ic_kava_22
        "ravencoin", "ravencoin/test" -> R.drawable.ic_ravencoin_22
        "cosmos", "cosmos/test" -> R.drawable.ic_cosmos_22
        "terra", "terra-luna" -> R.drawable.ic_terra_22
        "terra-2", "terra-luna-2" -> R.drawable.ic_terra2_22
        "cronos" -> R.drawable.ic_cronos_22
        "TELOS", "TELOS/test" -> R.drawable.ic_telos_22
        "aleph-zero", "aleph-zero/test" -> R.drawable.ic_azero_22
        "octaspace", "octaspace/test" -> R.drawable.ic_octaspace_22
        "chia", "chia/test" -> R.drawable.ic_chia_22
        "NEAR", "NEAR/test" -> R.drawable.ic_near_22
        "decimal", "decimal/test" -> R.drawable.ic_decimal_22
        "xdc", "xdc/test" -> R.drawable.ic_xdc_22
        "vechain", "vechain/test" -> R.drawable.ic_vechain_22
        "aptos", "aptos/test" -> R.drawable.ic_aptos_22
        "shibarium", "shibarium/test" -> R.drawable.ic_shibarium_22
        "algorand", "algorand/test" -> R.drawable.ic_algorand_22
        "hedera", "hedera/test" -> R.drawable.ic_hedera_22
        "playa3ull" -> R.drawable.ic_playa3ull_22
        "DUC" -> R.drawable.ic_ducatus_22
        "aurora", "aurora/test" -> R.drawable.ic_aurora_22
        "areon", "areon/test" -> R.drawable.ic_areon_22
        "pls", "pls/test" -> R.drawable.ic_pls_22
        "zkSyncEra", "zkSyncEra/test" -> R.drawable.ic_zksync_22
        "moonbeam", "moonbeam/test" -> R.drawable.ic_moonbeam_22
        "manta-pacific", "manta/test" -> R.drawable.ic_manta_22
        "polygonZkEVM", "polygonZkEVM/test" -> R.drawable.ic_polygon_22
        "moonriver", "moonriver/test" -> R.drawable.ic_moonriver_22
        "mantle", "mantle/test" -> R.drawable.ic_mantle_22
        "flare", "flare/test" -> R.drawable.ic_flare_22
        "taraxa", "taraxa/test" -> R.drawable.ic_taraxa_22
        "radiant" -> R.drawable.ic_radiant_22
        "base", "base/test" -> R.drawable.ic_base_22
        "joystream" -> R.drawable.ic_joystream_22
        "koinos", "koinos/test" -> R.drawable.ic_koinos_22
        "bittensor" -> R.drawable.ic_bittensor_22
        "blast", "blast/test" -> R.drawable.ic_blast_22
        "filecoin" -> R.drawable.ic_filecoin_22
        "cyber", "cyber/test" -> R.drawable.ic_cyber_22
        "sei", "sei/test" -> R.drawable.ic_sei_22
        "internet-computer" -> R.drawable.ic_icp_22
        "sui", "sui/test" -> R.drawable.ic_sui_22
        "energy-web-chain", "energy-web-chain/test" -> R.drawable.ic_energy_web_22
        "energy-web-x", "energy-web-x/test" -> R.drawable.ic_energy_web_22
        "core", "core/test" -> R.drawable.ic_core_22
        "casper", "casper/test" -> R.drawable.ic_casper_22
        "xodex" -> R.drawable.ic_xodex_22
        "canxium" -> R.drawable.ic_canxium_22
        "chiliz", "chiliz/test" -> R.drawable.ic_chiliz_22
        "alephium", "alephium/test" -> R.drawable.ic_alephium_22
        "clore-ai" -> R.drawable.ic_clore_22
        "fact0rn" -> R.drawable.ic_fact0rn_22
        "odyssey", "odyssey/test" -> R.drawable.ic_odyssey_chain_22
        "bitrock", "bitrock/test" -> R.drawable.ic_bitrock_22
        "sonic", "sonic/test" -> R.drawable.ic_sonic_22
        "apechain", "apechain/test" -> R.drawable.ic_apecoin_22
        "scroll", "scroll/test" -> R.drawable.ic_alert_24 // FIXME: add icon during full integration
        "zklink", "zklink/test" -> R.drawable.ic_zklink_22
        "vanar-chain", "vanar-chain/test" -> R.drawable.ic_vanar_22
        "pepecoin", "pepecoin/test" -> R.drawable.ic_pepecoin_22
        else -> R.drawable.ic_alert_24
    }
}