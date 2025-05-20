package com.tangem.domain.card

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.NetworkType
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Derivates an exteneded public key (xpub) based on blockchain hardened derivation
 */
class GetExtendedPublicKeyForCurrencyUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, String> {
        return Either.catch {
            val userWallet = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
                ?: error("Wallet not found")

            val blockchain = Blockchain.fromId(network.rawId)
            val isSecp256k1Blockchain = Blockchain.secp256k1Blockchains(network.isTestnet).contains(blockchain)

            val hdKey = if (isSecp256k1Blockchain) {
                userWallet.wallet.publicKey.derivationType?.hdKey ?: error("No derivation found")
            } else {
                error("No derivation found")
            }

            var childKey = makeChildKey(
                isBip44DerivationStyleXPUB = blockchain.isBip44DerivationStyleXPUB(),
                extendedPublicKey = hdKey.extendedPublicKey,
                derivationPath = hdKey.path,
            )

            var parentKey = Key(
                derivationPath = childKey.derivationPath.dropLastNodes(1),
                extendedPublicKey = null,
            )

            val pendingDerivations = getPendingDerivations(childKey, parentKey)
            val derivedKeys = deriveKeys(
                userWalletId = userWalletId,
                seedKey = userWallet.wallet.publicKey.seedKey,
                paths = pendingDerivations,
            )

            if (childKey.extendedPublicKey == null) {
                childKey = childKey.copy(
                    extendedPublicKey = derivedKeys[childKey.derivationPath] ?: error("Failed to derive child key"),
                )
            }

            if (parentKey.extendedPublicKey == null) {
                parentKey = parentKey.copy(
                    extendedPublicKey = derivedKeys[parentKey.derivationPath] ?: error("Failed to derive parent key"),
                )
            }

            makeExtendedKey(childKey, parentKey, network.isTestnet)
        }
    }

    /**
     * @return true if xpub generation is supported, false otherwise
     */
    suspend fun isSupported(userWalletId: UserWalletId, network: Network): Boolean {
        val userWallet = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
            ?: error("Wallet not found")

        val blockchain = Blockchain.fromId(network.rawId)
        val isSecp256k1Blockchain = Blockchain.secp256k1Blockchains(network.isTestnet).contains(blockchain)
        val isHdKey = userWallet.wallet.publicKey.derivationType?.hdKey

        return isSecp256k1Blockchain && isHdKey != null
    }

    private suspend fun deriveKeys(
        userWalletId: UserWalletId,
        seedKey: ByteArray,
        paths: MutableList<DerivationPath>,
    ): ExtendedPublicKeysMap {
        val result = derivationsRepository.derivePublicKeys(userWalletId, mapOf(ByteArrayKey(seedKey) to paths))
        return result.getValue(ByteArrayKey(seedKey))
    }

    private fun makeExtendedKey(childKey: Key, parentKey: Key, isTestnet: Boolean): String {
        val publicKey = childKey.extendedPublicKey?.publicKey ?: error("No public key found")
        val chainCode = childKey.extendedPublicKey.chainCode
        val lastChildNode = childKey.derivationPath.nodes.last()
        val parentPublicKey = parentKey.extendedPublicKey?.publicKey

        val depth = childKey.derivationPath.nodes.size
        val childNumber = lastChildNode.index
        val parentFingerprint = parentPublicKey
            ?.calculateSha256()?.calculateRipemd160()
            ?.take(PARENT_FINGERPRINT_SIZE)?.toByteArray()
            ?: error("No parent fingerprint found")

        val net = if (isTestnet) NetworkType.Testnet else NetworkType.Mainnet
        return ExtendedPublicKey(
            publicKey = publicKey,
            chainCode = chainCode,
            depth = depth,
            parentFingerprint = parentFingerprint,
            childNumber = childNumber,
        ).serialize(net)
    }

    private fun getPendingDerivations(childKey: Key, parentKey: Key): MutableList<DerivationPath> {
        val pendingDerivations = mutableListOf<DerivationPath>()

        if (childKey.extendedPublicKey == null) {
            pendingDerivations.add(childKey.derivationPath)
        }

        if (parentKey.extendedPublicKey == null) {
            pendingDerivations.add(parentKey.derivationPath)
        }

        return pendingDerivations
    }

    private fun makeChildKey(
        isBip44DerivationStyleXPUB: Boolean,
        extendedPublicKey: ExtendedPublicKey,
        derivationPath: DerivationPath,
    ): Key = if (isBip44DerivationStyleXPUB) {
        Key(derivationPath.dropLastNodes(2), null)
    } else {
        Key(derivationPath, extendedPublicKey)
    }

    private fun DerivationPath.dropLastNodes(count: Int): DerivationPath {
        return DerivationPath(nodes.dropLast(count))
    }

    private data class Key(
        val derivationPath: DerivationPath,
        val extendedPublicKey: ExtendedPublicKey?,
    )

    private companion object {
        const val PARENT_FINGERPRINT_SIZE = 4
    }
}