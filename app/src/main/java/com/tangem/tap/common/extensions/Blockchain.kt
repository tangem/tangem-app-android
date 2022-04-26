package com.tangem.tap.common.extensions

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.remove
import com.tangem.wallet.R

@DrawableRes
fun Blockchain.getRoundIconRes(): Int {
    return when (this) {
        Blockchain.Ducatus -> R.drawable.ic_ducatus
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet,-> R.drawable.ic_bitcoin_round
        Blockchain.BitcoinCash -> R.drawable.ic_bitcoin_cash_round
        Blockchain.Litecoin -> R.drawable.ic_litecoin_round
        Blockchain.Ethereum, Blockchain.EthereumTestnet, -> R.drawable.ic_eth_round
        Blockchain.RSK -> R.drawable.ic_rsk_round
        Blockchain.Cardano, Blockchain.CardanoShelley -> R.drawable.ic_cardano_round
        Blockchain.Tezos -> R.drawable.ic_tezos_round
        Blockchain.XRP -> R.drawable.ic_xrp_round
        Blockchain.Stellar -> R.drawable.ic_stellar_round
        Blockchain.Avalanche, Blockchain.AvalancheTestnet -> R.drawable.ic_avalanche_round
        Blockchain.Polygon, Blockchain.PolygonTestnet -> R.drawable.ic_polygon_round
        Blockchain.Solana, Blockchain.SolanaTestnet -> R.drawable.ic_solana_round
        Blockchain.Fantom, Blockchain.FantomTestnet -> R.drawable.ic_fantom_round
        Blockchain.BSC, Blockchain.BSCTestnet, Blockchain.Binance, Blockchain.BinanceTestnet -> R.drawable.ic_bsc_round
        Blockchain.Dogecoin -> R.drawable.ic_dogecoin_round
        else -> R.drawable.ic_tangem_logo
    }
}

@DrawableRes
fun Blockchain.getGreyedOutIconRes(): Int {
    return when (this) {
//        Blockchain.Ducatus -> R.drawable.ic_ducatus
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet,-> R.drawable.ic_bitcoin_no_color
        Blockchain.BitcoinCash -> R.drawable.ic_bitcoin_cash_no_color
        Blockchain.Litecoin -> R.drawable.ic_litecoin_no_color
        Blockchain.Ethereum, Blockchain.EthereumTestnet, -> R.drawable.ic_eth_no_color
        Blockchain.RSK -> R.drawable.ic_rsk_no_color
        Blockchain.Cardano, Blockchain.CardanoShelley -> R.drawable.ic_cardano_no_color
        Blockchain.Tezos -> R.drawable.ic_tezos_no_color
        Blockchain.XRP -> R.drawable.ic_xrp_no_color
        Blockchain.Stellar -> R.drawable.ic_stellar_no_color
        Blockchain.Avalanche, Blockchain.AvalancheTestnet -> R.drawable.ic_avalanche_no_color
        Blockchain.Polygon, Blockchain.PolygonTestnet -> R.drawable.ic_polygon_no_color
        Blockchain.Solana, Blockchain.SolanaTestnet -> R.drawable.ic_solana_no_color
        Blockchain.Fantom, Blockchain.FantomTestnet -> R.drawable.ic_fantom_no_color
        Blockchain.BSC, Blockchain.BSCTestnet, Blockchain.Binance, Blockchain.BinanceTestnet -> R.drawable.ic_bsc_no_color
        Blockchain.Dogecoin -> R.drawable.ic_dogecoin_no_color
        else -> R.drawable.ic_tangem_logo
    }
}

fun Blockchain.getNetworkName(): String {
    return when (this) {
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ERC20"
        Blockchain.BSC, Blockchain.BSCTestnet -> "BEP20"
        Blockchain.Binance, Blockchain.BinanceTestnet -> "BEP2"
        else -> ""
    }
}

val Blockchain.fullNameWithoutTestnet
get() = this.fullName.remove(" Testnet")