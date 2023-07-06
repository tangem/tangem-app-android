package com.tangem.feature.wallet.presentation.wallet.domain

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.CardTypesResolver
import com.tangem.feature.wallet.impl.R

/**
 * Wallet image resolver
 *
[REDACTED_AUTHOR]
 */
internal object WalletImageResolver {

    private const val DOUBLE_WALLET_SET_BACKUP_COUNT = 1
    private const val TRIPLE_WALLET_SET_BACKUP_COUNT = 2

    /** Get image by [cardTypesResolver] */
    @DrawableRes
    fun resolve(cardTypesResolver: CardTypesResolver): Int? {
        return when {
            cardTypesResolver.isWallet2() -> resolveWallet2(cardTypesResolver)
            cardTypesResolver.isTangemWallet() -> R.drawable.ill_wallet_120_106
            cardTypesResolver.isWhiteWallet() -> R.drawable.ill_old_wallet_120_106
            cardTypesResolver.isTangemTwins() -> R.drawable.ill_twin_120_106
            cardTypesResolver.isStart2Coin() -> R.drawable.ill_start2coin_120_106
            cardTypesResolver.isTangemNote() -> resolveNote(cardTypesResolver)
            cardTypesResolver.isDev() -> R.drawable.ill_dev_120_106
            else -> null
        }
    }

    private fun resolveWallet2(cardTypesResolver: CardTypesResolver): Int? {
        return when (cardTypesResolver.getBackupCardsCount()) {
            DOUBLE_WALLET_SET_BACKUP_COUNT -> R.drawable.ill_wallet2_cards2_120_106
            TRIPLE_WALLET_SET_BACKUP_COUNT -> R.drawable.ill_wallet2_cards3_120_106
            else -> null
        }
    }

    private fun resolveNote(cardTypesResolver: CardTypesResolver): Int? {
        return when (cardTypesResolver.getBlockchain()) {
            Blockchain.Bitcoin -> R.drawable.ill_note_btc_120_106
            Blockchain.Ethereum -> R.drawable.ill_note_ethereum_120_106
            Blockchain.Binance -> R.drawable.ill_note_binance_120_106
            Blockchain.Dogecoin -> R.drawable.ill_note_doge_120_106
            Blockchain.Cardano -> R.drawable.ill_note_cardano_120_106
            Blockchain.XRP -> R.drawable.ill_note_xrp_120_106
            else -> null
        }
    }
}