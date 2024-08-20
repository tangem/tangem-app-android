package com.tangem.feature.wallet.presentation.wallet.domain

import androidx.annotation.DrawableRes
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.impl.R

/**
 * Wallet image resolver
 *
[REDACTED_AUTHOR]
 */
internal object WalletImageResolver {

    private const val WALLET_WITHOUT_BACKUP_COUNT = 1
    private const val WALLET_WITH_ONE_BACKUP_COUNT = 2
    private const val WALLET_WITH_TWO_BACKUPS_COUNT = 3

    /** Get a specified wallet [userWallet] image */
    @Suppress("CyclomaticComplexMethod")
    @DrawableRes
    fun resolve(userWallet: UserWallet): Int? {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver

        val cobrandImage = Wallet2CobrandImage.entries.firstOrNull {
            it.batchIds.contains(userWallet.scanResponse.card.batchId)
        }

        val noteImage by lazy {
            val noteBlockchain = cardTypesResolver.getBlockchain()

            NoteImage.entries.firstOrNull { it.blockchain == noteBlockchain }
        }

        return when {
            cardTypesResolver.isDevKit() -> R.drawable.ill_dev_120_106
            cobrandImage != null -> userWallet.resolveWallet2Cobrand(image = cobrandImage)
            cardTypesResolver.isWallet2() -> userWallet.resolveWallet2()
            cardTypesResolver.isShibaWallet() -> userWallet.resolveShibaWallet()
            cardTypesResolver.isTangemWallet() -> userWallet.resolveWallet1()
            cardTypesResolver.isWhiteWallet() -> R.drawable.ill_wallet_old_white_120_106
            cardTypesResolver.isTangemTwins() -> R.drawable.ill_twins_120_106
            cardTypesResolver.isStart2Coin() -> R.drawable.ill_start2coin_120_106
            cardTypesResolver.isTangemNote() -> noteImage?.imageResId
            else -> null
        }
    }

    private fun UserWallet.resolveWallet2Cobrand(image: Wallet2CobrandImage): Int? {
        return resolveWallet2(oneBackupResId = image.cards2ResId, twoBackupResId = image.cards3ResId)
    }

    private fun UserWallet.resolveShibaWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_shiba_card2_120_106,
            twoBackupResId = R.drawable.ill_shiba_card3_120_106,
        )
    }

    private fun UserWallet.resolveWallet2(
        @DrawableRes oneBackupResId: Int = R.drawable.ill_wallet2_cards2_120_106,
        @DrawableRes twoBackupResId: Int = R.drawable.ill_wallet2_cards3_120_106,
    ): Int? {
        return resolveWalletWithBackups { count ->
            if (DemoConfig().isDemoCardId(cardId)) return@resolveWalletWithBackups oneBackupResId

            when (count) {
                WALLET_WITH_ONE_BACKUP_COUNT -> oneBackupResId
                WALLET_WITH_TWO_BACKUPS_COUNT -> twoBackupResId
                else -> null
            }
        }
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
}