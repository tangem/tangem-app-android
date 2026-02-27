package com.tangem.tap.domain.tasks.product

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.wallets.derivations.BlockchainToDerive
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.tap.features.demo.DemoHelper
import javax.inject.Inject

/**
 * Finder of blockchains to derive.
 * Returns only saved, default or demo blockchains without any additional logic
 * (no cardano/ethereum additions or unnecessary blockchain removals).
 */
class BlockchainToDeriveFinder @Inject constructor(
    private val walletAccountsFetcher: WalletAccountsFetcher,
) {

    suspend fun find(card: CardDTO): Set<BlockchainToDerive> {
        if (!card.settings.isHDWalletAllowed || card.wallets.isEmpty()) return emptySet()
        val userWalletId = UserWalletIdBuilder.card(card).build() ?: return emptySet()

        val derivationStyle = card.derivationStyleProvider.getDerivationStyle()

        val blockchains = getBlockchains(userWalletId).ifEmpty {
            if (DemoHelper.isDemoCardId(card.cardId)) {
                getDemoBlockchains(derivationStyle, card.cardId)
            } else {
                getDefaultBlockchains(derivationStyle)
            }
        }

        return blockchains
    }

    private suspend fun getBlockchains(userWalletId: UserWalletId): Set<BlockchainToDerive> {
        return walletAccountsFetcher.getSaved(userWalletId)?.accounts.orEmpty()
            .flatMap { accountDTO ->
                accountDTO.tokens.orEmpty()
                    .filter { it.contractAddress == null }
            }
            .mapNotNull { coin ->
                val blockchain = Blockchain.fromNetworkId(coin.networkId) ?: return@mapNotNull null
                val derivationPath = coin.derivationPath?.let(::DerivationPath) ?: return@mapNotNull null

                BlockchainToDerive(blockchain, derivationPath)
            }
            .toSet()
    }

    private fun getDemoBlockchains(derivationStyle: DerivationStyle?, cardId: String): Set<BlockchainToDerive> {
        return DemoHelper.config.getDemoBlockchains(cardId).mapToBlockchainsWithDerivations(derivationStyle)
    }

    private fun getDefaultBlockchains(derivationStyle: DerivationStyle?): Set<BlockchainToDerive> {
        val defaultBlockchains = setOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        return defaultBlockchains.mapToBlockchainsWithDerivations(derivationStyle)
    }

    private fun Set<Blockchain>.mapToBlockchainsWithDerivations(
        derivationStyle: DerivationStyle?,
    ): Set<BlockchainToDerive> {
        return mapNotNullTo(hashSetOf()) { blockchain ->
            val derivationPath = blockchain.derivationPath(derivationStyle) ?: return@mapNotNullTo null
            BlockchainToDerive(blockchain, derivationPath)
        }
    }
}