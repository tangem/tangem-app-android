package com.tangem.feature.wallet.presentation.wallet.domain

import androidx.annotation.DrawableRes
import com.tangem.blockchain.common.Blockchain
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
// TODO: make flexible to integrate cobrands ([REDACTED_JIRA])
internal object WalletImageResolver {

    private const val WALLET_WITHOUT_BACKUP_COUNT = 1
    private const val WALLET_WITH_ONE_BACKUP_COUNT = 2
    private const val WALLET_WITH_TWO_BACKUPS_COUNT = 3

    /** Get a specified wallet [userWallet] image */
    @Suppress("CyclomaticComplexMethod")
    @DrawableRes
    fun resolve(userWallet: UserWallet): Int? {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        return when {
            cardTypesResolver.isDevKit() -> R.drawable.ill_dev_120_106
            cardTypesResolver.isWhiteWallet2() -> userWallet.resolveWhiteWallet2()
            cardTypesResolver.isAvroraWallet() -> userWallet.resolveAvroraWallet()
            cardTypesResolver.isTraillantWallet() -> userWallet.resolveTraillantWallet()
            cardTypesResolver.isTronWallet() -> userWallet.resolveTronWallet()
            cardTypesResolver.isKaspaWallet() -> userWallet.resolveKaspaWallet()
            cardTypesResolver.isKaspa2Wallet() -> userWallet.resolveKaspa2Wallet()
            cardTypesResolver.isKaspaResellerWallet() -> userWallet.resolveKaspaResellerWallet()
            cardTypesResolver.isBadWallet() -> userWallet.resolveBadWallet()
            cardTypesResolver.isJrWallet() -> userWallet.resolveJrWallet()
            cardTypesResolver.isGrimWallet() -> userWallet.resolveGrimWallet()
            cardTypesResolver.isSatoshiFriendsWallet() -> userWallet.resolveSatoshiWallet()
            cardTypesResolver.isBitcoinPizzaDayWallet() -> userWallet.resolveBitcoinPizzaDayWallet()
            cardTypesResolver.isVeChainWallet() -> userWallet.resolveVeChainWallet()
            cardTypesResolver.isNewWorldEliteWallet() -> userWallet.resolveNewWorldEliteWallet()
            cardTypesResolver.isRedPandaWallet() -> userWallet.resolveRedPandaWallet()
            cardTypesResolver.isCryptoSethWallet() -> userWallet.resolveCryptoSethWallet()
            cardTypesResolver.isKishuInuWallet() -> userWallet.resolveKishuInuWallet()
            cardTypesResolver.isBabyDogeWallet() -> userWallet.resolveBabyDogeWallet()
            cardTypesResolver.isCOQWallet() -> userWallet.resolveCOQWallet()
            cardTypesResolver.isCoinMetricaWallet() -> userWallet.resolveCoinMetricaWallet()
            cardTypesResolver.isVoltInuWallet() -> userWallet.resolveVoltInuWallet()
            cardTypesResolver.isVividWallet() -> userWallet.resolveVividWallet()
            cardTypesResolver.isPastelWallet() -> userWallet.resolvePastelWallet()
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
            if (DemoConfig().isDemoCardId(cardId)) return@resolveWalletWithBackups oneBackupResId

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

    private fun UserWallet.resolveKaspa2Wallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_kaspa2_card2_120_106,
            twoBackupResId = R.drawable.ill_kaspa2_card3_120_106,
        )
    }

    private fun UserWallet.resolveKaspaResellerWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_kaspa_reseller_card2_120_106,
            twoBackupResId = R.drawable.ill_kaspa_reseller_card3_120_106,
        )
    }

    private fun UserWallet.resolveBadWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_bad_card2_120_106,
            twoBackupResId = R.drawable.ill_bad_card3_120_106,
        )
    }

    private fun UserWallet.resolveJrWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_jr_card2_120_106,
            twoBackupResId = R.drawable.ill_jr_card3_120_106,
        )
    }

    private fun UserWallet.resolveGrimWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_grim_card2_120_106,
            twoBackupResId = R.drawable.ill_grim_card3_120_106,
        )
    }

    private fun UserWallet.resolveSatoshiWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_satoshi_card2_120_106,
            twoBackupResId = R.drawable.ill_satoshi_card3_120_106,
        )
    }

    private fun UserWallet.resolveShibaWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_shiba_card2_120_106,
            twoBackupResId = R.drawable.ill_shiba_card3_120_106,
        )
    }

    private fun UserWallet.resolveWhiteWallet2(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_white_card2_120_106,
            twoBackupResId = R.drawable.ill_white_card3_120_106,
        )
    }

    private fun UserWallet.resolveAvroraWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_avrora_card2_120_106,
            twoBackupResId = R.drawable.ill_avrora_card3_120_106,
        )
    }

    private fun UserWallet.resolveTraillantWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_traillant_card2_120_106,
            twoBackupResId = R.drawable.ill_traillant_card3_120_106,
        )
    }

    private fun UserWallet.resolveBitcoinPizzaDayWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_pizza_day_card2_120_106,
            twoBackupResId = R.drawable.ill_pizza_day_card3_120_106,
        )
    }

    private fun UserWallet.resolveVeChainWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_vechain_card2_120_106,
            twoBackupResId = R.drawable.ill_vechain_card3_120_106,
        )
    }

    private fun UserWallet.resolveNewWorldEliteWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_nwe_card2_120_106,
            twoBackupResId = R.drawable.ill_nwe_card3_120_106,
        )
    }

    private fun UserWallet.resolveRedPandaWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_red_panda_card2_120_106,
            twoBackupResId = R.drawable.ill_red_panda_card3_120_106,
        )
    }

    private fun UserWallet.resolveCryptoSethWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_crypto_seth_card2_120_106,
            twoBackupResId = R.drawable.ill_crypto_seth_card3_120_106,
        )
    }

    private fun UserWallet.resolveKishuInuWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_kishu_inu_card2_120_106,
            twoBackupResId = R.drawable.ill_kishu_inu_card3_120_106,
        )
    }

    private fun UserWallet.resolveBabyDogeWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_baby_doge_card2_120_106,
            twoBackupResId = R.drawable.ill_baby_doge_card3_120_106,
        )
    }

    private fun UserWallet.resolveCOQWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_coq_card2_120_106,
            twoBackupResId = R.drawable.ill_coq_card3_120_106,
        )
    }

    private fun UserWallet.resolveCoinMetricaWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_coin_metrica_card2_120_106,
            twoBackupResId = R.drawable.ill_coin_metrica_card3_120_106,
        )
    }

    private fun UserWallet.resolveVoltInuWallet(): Int? {
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_volt_inu_card2_120_106,
            twoBackupResId = R.drawable.ill_volt_inu_card3_120_106,
        )
    }

    private fun UserWallet.resolveVividWallet(): Int? {
        // for multicolored cards use image of 3 cards in all cases
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_vivid_cards3_120_106,
            twoBackupResId = R.drawable.ill_vivid_cards3_120_106,
        )
    }

    private fun UserWallet.resolvePastelWallet(): Int? {
        // for multicolored cards use image of 3 cards in all cases
        return resolveWallet2(
            oneBackupResId = R.drawable.ill_pastel_cards3_120_106,
            twoBackupResId = R.drawable.ill_pastel_cards3_120_106,
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