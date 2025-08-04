package com.tangem.tap.domain.tasks.product

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.card.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal data class BlockchainToDerive(
    val blockchain: Blockchain,
    val derivationPath: DerivationPath?,
)

// FIXME: May be move to DI, currently unnecessary
internal class DerivationsFinder(
    private val userTokensResponseStore: UserTokensResponseStore,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun findBlockchainsToDerive(
        card: CardDTO,
        derivationStyleProvider: DerivationStyleProvider,
    ): Set<BlockchainToDerive> {
        if (!card.settings.isHDWalletAllowed || card.wallets.isEmpty()) return emptySet()
        val userWalletId = UserWalletIdBuilder.card(card).build() ?: return emptySet()
        val derivationStyle = derivationStyleProvider.getDerivationStyle()

        var blockchains = withContext(dispatchers.io) {
            getBlockchains(userWalletId)
        }

        if (blockchains.isEmpty()) {
            blockchains = if (DemoHelper.isDemoCardId(card.cardId)) {
                getDemoBlockchains(derivationStyle)
            } else {
                getDefaultBlockchains(derivationStyle)
            }
        }

        // we should generate second key for cardano
        // because cardano address generation for wallet2 requires keys from 2 derivations
        // https://developers.cardano.org/docs/get-started/cardano-serialization-lib/generating-keys/
        blockchains.addSecondCardanoDerivationIfPresent()

        if (card.settings.isHDWalletAllowed) {
            blockchains.addEthereumBlockchains(derivationStyle)
        }

        // pay attention to this
        if (!card.useOldStyleDerivation) {
            blockchains.removeUnnecessaryBlockchains()
        }

        return blockchains
    }

    private suspend fun getBlockchains(userWalletId: UserWalletId): MutableSet<BlockchainToDerive> {
        val responseTokens = userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)?.tokens
            ?: return hashSetOf()

        return responseTokens.asSequence()
            .filter { it.contractAddress == null }
            .mapNotNull { coin ->
                val blockchain = Blockchain.fromNetworkId(coin.networkId) ?: return@mapNotNull null
                val derivationPath = coin.derivationPath?.let(::DerivationPath)

                BlockchainToDerive(blockchain, derivationPath)
            }
            .toMutableSet()
    }

    // TODO: Move to user wallet config
    private fun getDemoBlockchains(derivationStyle: DerivationStyle?): MutableSet<BlockchainToDerive> {
        return DemoHelper.config.demoBlockchains.mapToBlockchainsWithDerivations(derivationStyle)
    }

    // TODO: Move to user wallet config
    private fun getDefaultBlockchains(derivationStyle: DerivationStyle?): MutableSet<BlockchainToDerive> {
        val defaultBlockchains = setOf(Blockchain.Bitcoin, Blockchain.Ethereum)

        return defaultBlockchains.mapToBlockchainsWithDerivations(derivationStyle)
    }
}

private fun MutableSet<BlockchainToDerive>.addEthereumBlockchains(derivationStyle: DerivationStyle?) {
    val ethereumBlockchains = setOf(Blockchain.Ethereum, Blockchain.EthereumTestnet)
        .mapToBlockchainsWithDerivations(derivationStyle)

    addAll(ethereumBlockchains)
}

private fun MutableSet<BlockchainToDerive>.removeUnnecessaryBlockchains() {
    val unnecessaryBlockchains = listOf(
        Blockchain.BSC, Blockchain.BSCTestnet,
        Blockchain.Polygon, Blockchain.PolygonTestnet,
        Blockchain.RSK,
        Blockchain.Fantom, Blockchain.FantomTestnet,
        Blockchain.Avalanche, Blockchain.AvalancheTestnet,
    )

    removeAll { it.blockchain in unnecessaryBlockchains }
}

private fun MutableSet<BlockchainToDerive>.addSecondCardanoDerivationIfPresent() {
    val cardanoDerivation = this
        .firstOrNull { it.blockchain == Blockchain.Cardano }
        ?.derivationPath
        ?: return

    val secondCardanoBlockchain = BlockchainToDerive(
        blockchain = Blockchain.Cardano,
        derivationPath = CardanoUtils.extendedDerivationPath(cardanoDerivation),
    )

    add(secondCardanoBlockchain)
}

private fun Set<Blockchain>.mapToBlockchainsWithDerivations(
    derivationStyle: DerivationStyle?,
): MutableSet<BlockchainToDerive> {
    return mapTo(hashSetOf()) { blockchain ->
        BlockchainToDerive(blockchain, blockchain.derivationPath(derivationStyle))
    }
}