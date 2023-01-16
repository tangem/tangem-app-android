package com.tangem.tap.common.extensions

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.remove
import com.tangem.wallet.R

@Suppress("ComplexMethod")
@DrawableRes
fun Blockchain.getGreyedOutIconRes(): Int {
    return when (this) {
        Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> R.drawable.ic_arbitrum_no_color
//        Blockchain.Ducatus -> R.drawable.ic_ducatus
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> R.drawable.ic_bitcoin_no_color
        Blockchain.BitcoinCash -> R.drawable.ic_bitcoin_cash_no_color
        Blockchain.Litecoin -> R.drawable.ic_litecoin_no_color
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> R.drawable.ic_eth_no_color
        Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> R.drawable.ic_eth_no_color
        Blockchain.RSK -> R.drawable.ic_rsk_no_color
        Blockchain.Cardano, Blockchain.CardanoShelley -> R.drawable.ic_cardano_no_color
        Blockchain.Tezos -> R.drawable.ic_tezos_no_color
        Blockchain.XRP -> R.drawable.ic_xrp_no_color
        Blockchain.Stellar -> R.drawable.ic_stellar_no_color
        Blockchain.Avalanche, Blockchain.AvalancheTestnet -> R.drawable.ic_avalanche_no_color
        Blockchain.Polygon, Blockchain.PolygonTestnet -> R.drawable.ic_polygon_no_color
        Blockchain.Solana, Blockchain.SolanaTestnet -> R.drawable.ic_solana_no_color
        Blockchain.Fantom, Blockchain.FantomTestnet -> R.drawable.ic_fantom_no_color
        Blockchain.BSC, Blockchain.BSCTestnet, Blockchain.Binance, Blockchain.BinanceTestnet ->
            R.drawable.ic_bsc_no_color
        Blockchain.Dogecoin -> R.drawable.ic_dogecoin_no_color
        Blockchain.Tron, Blockchain.TronTestnet -> R.drawable.ic_tron_no_color
        Blockchain.Gnosis -> R.drawable.ic_gnosis_no_color
        Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> R.drawable.ic_ethereumpow_no_color
        Blockchain.EthereumFair -> R.drawable.ic_ethereumfair_no_color
        Blockchain.Polkadot, Blockchain.PolkadotTestnet -> R.drawable.ic_polkadot_no_color
        Blockchain.Kusama -> R.drawable.ic_kusama_no_color
        Blockchain.Optimism, Blockchain.OptimismTestnet -> R.drawable.ic_optimism_no_color
        Blockchain.Dash -> R.drawable.ic_dash_no_color
        else -> R.drawable.ic_tangem_logo
    }
}

fun Blockchain.getNetworkName(): String {
    return when (this) {
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ERC20"
        Blockchain.BSC, Blockchain.BSCTestnet -> "BEP20"
        Blockchain.Binance, Blockchain.BinanceTestnet -> "BEP2"
        Blockchain.Tron, Blockchain.TronTestnet -> "TRC20"
        else -> ""
    }
}

val Blockchain.fullNameWithoutTestnet
    get() = this.fullName.remove(" Testnet")
