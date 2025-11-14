package com.tangem.data.common.tokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.card.common.TapWorkarounds.isTestCard
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.wallet.UserWallet

/**
 * Returns the default blockchains for the multi-currency wallet.
 *
 * @param userWallet The user's wallet, which can be either a cold or hot wallet.
 * @param demoConfig Configuration for demo cards, which may specify different default blockchains.
 */
fun getDefaultWalletBlockchains(userWallet: UserWallet, demoConfig: DemoConfig): Collection<Blockchain> {
    return when (userWallet) {
        is UserWallet.Cold -> {
            val card = userWallet.scanResponse.card

            var blockchainsInternal = if (demoConfig.isDemoCardId(card.cardId)) {
                demoConfig.getDemoBlockchains(card.cardId)
            } else {
                listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
            }

            if (card.isTestCard) {
                blockchainsInternal = blockchainsInternal.mapNotNull { it.getTestnetVersion() }
            }

            blockchainsInternal
        }
        is UserWallet.Hot -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
    }
}