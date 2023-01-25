package com.tangem.core.ui.extensions

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R

@Suppress("ComplexMethod")
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
        "ETH-Fair" -> R.drawable.img_eth_fair_22
        "Polkadot", "Polkadot/test" -> R.drawable.img_polkadot_22
        "Kusama" -> R.drawable.img_kusama_22
        "OPTIMISM", "OPTIMISM/test" -> R.drawable.img_optimism_22
        "DASH" -> R.drawable.img_dash_22
        else -> R.drawable.ic_alert_24
    }
}

@Suppress("ComplexMethod")
@DrawableRes
fun getActiveIconResByCoinId(coinId: String, networkId: String): Int {
    return when (coinId) {
        "binancecoin" -> R.drawable.img_bsc_22
        "bitcoin" -> R.drawable.img_btc_22
        "bitcoin-cash" -> R.drawable.img_btc_cash_22
        "ethereum" -> {
            when (networkId) {
                "ethereum" -> R.drawable.img_eth_22
                "arbitrum-one" -> R.drawable.img_arbitrum_22
                "optimistic-ethereum" -> R.drawable.img_optimism_22
                else -> R.drawable.ic_alert_24
            }
        }
        "ethereum-classic" -> R.drawable.img_eth_classic_22
        "stellar" -> R.drawable.img_stellar_22
        "cardano" -> R.drawable.img_cardano_22
        "matic-network" -> R.drawable.img_polygon_22
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
        "ethereumfair" -> R.drawable.img_eth_fair_22
        "kusama" -> R.drawable.img_kusama_22
        "dash" -> R.drawable.img_dash_22
        else -> R.drawable.ic_alert_24
    }
}
