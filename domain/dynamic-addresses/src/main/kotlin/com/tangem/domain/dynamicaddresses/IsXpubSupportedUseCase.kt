package com.tangem.domain.dynamicaddresses

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade

/**
 * Checks if XPUB generation is supported for the given wallet and network (hardware capability check).
 */
class IsXpubSupportedUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = network.toBlockchain()
        if (!DynamicAddressesSupportedBlockchains.isSupported(blockchain)) return false

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network) ?: return false
        return walletManager.wallet.publicKey.derivationType?.hdKey != null
    }
}