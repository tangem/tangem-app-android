package com.tangem.feature.wallet.presentation.wallet.domain

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.impl.R

/**
 * Wallet image resolver
 *
* [REDACTED_AUTHOR]
 */
internal object WalletImageResolver {

    private const val WALLET_WITHOUT_BACKUP_COUNT = 1
    private const val WALLET_WITH_ONE_BACKUP_COUNT = 2
    private const val WALLET_WITH_TWO_BACKUPS_COUNT = 3

    /** Get a specified wallet [userWallet] image */
    @DrawableRes
    fun resolve(userWallet: UserWallet): Int? {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        return when {
            cardTypesResolver.isDevKit() -> R.drawable.ill_dev_120_106
            cardTypesResolver.isTronWallet() -> userWallet.resolveTronWallet()
            cardTypesResolver.isKaspaWallet() -> userWallet.resolveKaspaWallet()
            cardTypesResolver.isBadWallet() -> userWallet.resolveBadWallet()
            cardTypesResolver.isWallet2() -> userWallet.resolveWallet2()
            cardTypesResolver.isShibaWallet() -> userWallet.resolveShibaWallet()
            cardTypesResolver.isTangemWallet() -> userWallet.resolveWallet1()
            cardTypesResolver.isWhiteWallet() -> R.drawable.ill_wallet_old_white_120_106
            cardTypesResolver.isTangemTwins() -> R.drawable.ill_twins_120_106
            cardTypesResolver.isStart2Coin() -> R.drawable.ill_start2coin_120_106
            cardTypesResolver.isTangemNote() -> resolveNote(blockchain = cardTypesResolver.getBlockchain())
            else -> null
        }
    }

    private fun UserWallet.resolveWallet2(
        @DrawableRes oneBackupResId: Int = R.drawable.ill_wallet2_cards2_120_106,
        @DrawableRes twoBackupResId: Int = R.drawable.ill_wallet2_cards3_120_106,
    ): Int? {
        return resolveWalletWithBackups { count ->
            when (count) {
                WALLET_WITH_ONE_BACKUP_COUNT -> oneBackupResId
                WALLET_WITH_TWO_BACKUPS_COUNT -> twoBackupResId
                else -> null
            }
        }
    }

    private fun UserWallet.resolveTronWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_tron_card2_120_106,
            twoBackupResId = R.drawable.ill_tron_card3_120_106,
        )
    }

    private fun UserWallet.resolveKaspaWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_kaspa_card2_120_106,
            twoBackupResId = R.drawable.ill_kaspa_card3_120_106,
        )
    }

    private fun UserWallet.resolveBadWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_bad_card2_120_106,
            twoBackupResId = R.drawable.ill_bad_card3_120_106,
        )
    }

    private fun UserWallet.resolveShibaWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_shiba_card2_120_106,
            twoBackupResId = R.drawable.ill_shiba_card3_120_106,
        )
    }

    private fun UserWallet.resolveWallet1(): Int? {
        return resolveWalletWithBackups { count ->
            when (count) {
                WALLET_WITHOUT_BACKUP_COUNT -> R.drawable.ill_wallet1_cards1_120_106
                WALLET_WITH_ONE_BACKUP_COUNT -> R.drawable.ill_wallet1_cards2_120_106
                WALLET_WITH_TWO_BACKUPS_COUNT -> R.drawable.ill_wallet1_cards3_120_106
                else -> null
            }
        }
    }

    private fun UserWallet.resolveWalletWithBackups(resolve: (Int) -> Int?): Int? {
        val count = getCardsCount()

        return if (count != null) resolve(count) else null
    }

    private fun resolveNote(blockchain: Blockchain): Int? {
        return when (blockchain) {
            Blockchain.Bitcoin -> R.drawable.ill_note_btc_120_106
            Blockchain.Ethereum -> R.drawable.ill_note_ethereum_120_106
            Blockchain.BSC -> R.drawable.ill_note_binance_120_106
            Blockchain.Dogecoin -> R.drawable.ill_note_doge_120_106
            Blockchain.Cardano -> R.drawable.ill_note_cardano_120_106
            Blockchain.XRP -> R.drawable.ill_note_xrp_120_106
            else -> null
        }
    }
}
