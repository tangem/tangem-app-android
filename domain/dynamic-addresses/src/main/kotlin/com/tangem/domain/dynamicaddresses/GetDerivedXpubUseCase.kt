package com.tangem.domain.dynamicaddresses

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.NetworkType
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository

/**
 * Returns the XPUB string if account-level keys are already derived (no card scan needed),
 * or null if keys are not available.
 */
class GetDerivedXpubUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val derivationsRepository: DerivationsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): String? {
        val blockchain = network.toBlockchain()
        if (!DynamicAddressesSupportedBlockchains.isSupported(blockchain)) return null
        if (!blockchain.isBip44DerivationStyleXPUB()) return null

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network) ?: return null
        val hdKey = walletManager.wallet.publicKey.derivationType?.hdKey ?: return null
        if (hdKey.path.nodes.size <= ACCOUNT_PATH_DROP_COUNT) return null

        val seedKey = ByteArrayKey(walletManager.wallet.publicKey.seedKey)
        val existingKeys = derivationsRepository.getExistingDerivedKeys(userWalletId, seedKey)

        val accountPath = DerivationPath(hdKey.path.nodes.dropLast(ACCOUNT_PATH_DROP_COUNT))
        val parentPath = DerivationPath(accountPath.nodes.dropLast(1))

        val childExtKey = existingKeys[accountPath] ?: return null
        val parentExtKey = existingKeys[parentPath] ?: return null

        val parentFingerprint = parentExtKey.publicKey
            .calculateSha256().calculateRipemd160()
            .take(PARENT_FINGERPRINT_SIZE).toByteArray()

        val net = if (blockchain.isTestnet()) NetworkType.Testnet else NetworkType.Mainnet
        return ExtendedPublicKey(
            publicKey = childExtKey.publicKey,
            chainCode = childExtKey.chainCode,
            depth = accountPath.nodes.size,
            parentFingerprint = parentFingerprint,
            childNumber = accountPath.nodes.last().index,
        ).serialize(net)
    }

    private companion object {
        const val ACCOUNT_PATH_DROP_COUNT = 2
        const val PARENT_FINGERPRINT_SIZE = 4
    }
}