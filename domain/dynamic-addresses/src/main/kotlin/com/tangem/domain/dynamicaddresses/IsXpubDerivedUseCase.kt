package com.tangem.domain.dynamicaddresses

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository

/**
 * Checks if the account-level XPUB key is already derived (no card scan needed).
 */
class IsXpubDerivedUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val derivationsRepository: DerivationsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = network.toBlockchain()
        if (!DynamicAddressesSupportedBlockchains.isSupported(blockchain)) return false
        if (!blockchain.isBip44DerivationStyleXPUB()) return false

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network) ?: return false
        val hdKey = walletManager.wallet.publicKey.derivationType?.hdKey ?: return false
        if (hdKey.path.nodes.size <= ACCOUNT_PATH_DROP_COUNT) return false
        val accountPath = DerivationPath(hdKey.path.nodes.dropLast(ACCOUNT_PATH_DROP_COUNT))

        val seedKey = ByteArrayKey(walletManager.wallet.publicKey.seedKey)
        val existingKeys = derivationsRepository.getExistingDerivedKeys(userWalletId, seedKey)
        return existingKeys[accountPath] != null
    }

    private companion object {
        const val ACCOUNT_PATH_DROP_COUNT = 2
    }
}