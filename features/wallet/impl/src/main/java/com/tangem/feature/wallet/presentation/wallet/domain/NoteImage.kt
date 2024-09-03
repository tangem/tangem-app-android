package com.tangem.feature.wallet.presentation.wallet.domain

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.feature.wallet.impl.R

/**
 * Note image model
 *
 * @property blockchain blockchain
 * @property imageResId image res id
 *
[REDACTED_AUTHOR]
 */
internal enum class NoteImage(
    val blockchain: Blockchain,
    @DrawableRes val imageResId: Int,
) {

    Bitcoin(blockchain = Blockchain.Bitcoin, imageResId = R.drawable.ill_note_btc_120_106),

    Ethereum(blockchain = Blockchain.Ethereum, imageResId = R.drawable.ill_note_ethereum_120_106),

    Binance(blockchain = Blockchain.BSC, imageResId = R.drawable.ill_note_binance_120_106),

    Dogecoin(blockchain = Blockchain.Dogecoin, imageResId = R.drawable.ill_note_doge_120_106),

    Cardano(blockchain = Blockchain.Cardano, imageResId = R.drawable.ill_note_cardano_120_106),

    XRP(blockchain = Blockchain.XRP, imageResId = R.drawable.ill_note_xrp_120_106),
}