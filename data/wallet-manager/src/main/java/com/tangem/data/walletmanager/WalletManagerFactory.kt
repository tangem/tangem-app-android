package com.tangem.data.walletmanager

import com.tangem.blockchain.blockchains.ethereum.SafeEthereumWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.walletmanager.extensions.makePublicKey
import com.tangem.data.walletmanager.extensions.makeWalletManagerForApp
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.utils.logging.TangemLogger

internal class WalletManagerFactory(
    private val blockchainSDKFactory: BlockchainSDKFactory,
) {

    suspend fun createWalletManager(
        scanResponse: ScanResponse,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
    ): WalletManager? {
        val derivationParams = getDerivationParams(derivationPath, scanResponse.derivationStyleProvider)

        return try {
            blockchainSDKFactory.getWalletManagerFactorySync()?.makeWalletManagerForApp(
                scanResponse = scanResponse,
                blockchain = blockchain,
                derivationParams = derivationParams,
            )
        } catch (e: Throwable) {
            TangemLogger.e("Failed to create wallet manager for $blockchain", e)
            null
        }
    }

    suspend fun createWalletManagerForHot(
        hotWallet: UserWallet.Hot,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
    ): WalletManager? {
        val curve = hotWallet.curvesConfig.primaryCurve(blockchain)
        val selectedWallet = hotWallet.wallets.orEmpty().firstOrNull { it.curve == curve }
            ?: return null
        return try {
            val factory = blockchainSDKFactory.getWalletManagerFactorySync() ?: return null

            if (derivationPath == null) {
                factory.createLegacyWalletManager(
                    blockchain = blockchain,
                    walletPublicKey = selectedWallet.publicKey,
                    curve = selectedWallet.curve,
                )
            } else {
                factory.createWalletManager(
                    blockchain = blockchain,
                    publicKey = makePublicKey(
                        seedKey = selectedWallet.publicKey,
                        blockchain = blockchain,
                        derivationPath = derivationPath,
                        derivedWalletKeys = selectedWallet.derivedKeys,
                        isWallet2 = true,
                    ) ?: return null,
                    curve = selectedWallet.curve,
                )
            }
        } catch (e: Throwable) {
            TangemLogger.w("Failed to create wallet manager for $blockchain", e)
            null
        }
    }

    /**
     * The manager of a joint account's Safe in [blockchain]: built on the wallet's owner key at [derivationPath],
     * the key that signs the Safe's operations, while every read is made for [safeAddress]. `null` while the owner
     * key is not derived on this wallet or the SDK cannot serve the network.
     */
    suspend fun createSafeWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        derivationPath: DerivationPath,
        safeAddress: String,
    ): SafeEthereumWalletManager? {
        val factory = blockchainSDKFactory.getWalletManagerFactorySync() ?: return null
        val publicKey = ownerPublicKey(userWallet, blockchain, derivationPath) ?: return null

        return try {
            factory.createSafeWalletManager(blockchain = blockchain, publicKey = publicKey, safeAddress = safeAddress)
        } catch (e: Throwable) {
            TangemLogger.e("Failed to create the Safe wallet manager for $blockchain", e)
            null
        }
    }

    private fun ownerPublicKey(
        userWallet: UserWallet,
        blockchain: Blockchain,
        derivationPath: DerivationPath,
    ): Wallet.PublicKey? = when (userWallet) {
        is UserWallet.Hot -> {
            val curve = userWallet.curvesConfig.primaryCurve(blockchain)
            val wallet = userWallet.wallets.orEmpty().firstOrNull { it.curve == curve } ?: return null

            makePublicKey(
                seedKey = wallet.publicKey,
                blockchain = blockchain,
                derivationPath = derivationPath,
                derivedWalletKeys = wallet.derivedKeys,
                isWallet2 = true,
            )
        }
        is UserWallet.Cold -> {
            val scanResponse = userWallet.scanResponse
            val wallet = scanResponse.card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: return null
            val seedKey = wallet.publicKey ?: return null
            val derivedKeys = scanResponse.derivedKeys[seedKey.toMapKey()] ?: return null

            makePublicKey(
                seedKey = seedKey,
                blockchain = blockchain,
                derivationPath = derivationPath,
                derivedWalletKeys = derivedKeys,
                isWallet2 = scanResponse.cardTypesResolver.isWallet2(),
            )
        }
    }

    private fun getDerivationParams(
        derivationPath: DerivationPath?,
        derivationStyleProvider: DerivationStyleProvider,
    ): DerivationParams? {
        val derivationStyle = derivationStyleProvider.getDerivationStyle() ?: return null

        return if (derivationPath == null) {
            DerivationParams.Default(derivationStyle)
        } else {
            DerivationParams.Custom(derivationPath)
        }
    }
}