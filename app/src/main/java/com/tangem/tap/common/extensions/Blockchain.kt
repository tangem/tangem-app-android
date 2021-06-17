package com.tangem.tap.common.extensions

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.wallet.R

@DrawableRes
fun Blockchain.getIconRes(): Int {
    return when (this) {
        Blockchain.Unknown -> R.drawable.shape_circle
        Blockchain.Ducatus -> R.drawable.ic_ducatus
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet,-> R.drawable.ic_btc
        Blockchain.BitcoinCash -> R.drawable.ic_btc_cash
        Blockchain.Litecoin -> R.drawable.ic_ltc
        Blockchain.Ethereum, Blockchain.EthereumTestnet, -> R.drawable.ic_eth
        Blockchain.RSK -> R.drawable.ic_rsk
        Blockchain.Cardano, Blockchain.CardanoShelley -> R.drawable.ic_cardano
        Blockchain.Binance, Blockchain.BinanceTestnet -> R.drawable.ic_binance
        Blockchain.Tezos -> R.drawable.ic_tezos
        Blockchain.XRP -> R.drawable.ic_xrp
        Blockchain.Stellar -> R.drawable.ic_stellar
        else -> R.drawable.shape_circle
    }
}