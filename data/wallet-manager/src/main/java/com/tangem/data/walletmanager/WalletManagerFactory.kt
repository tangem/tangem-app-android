package com.tangem.data.walletmanager

import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.walletmanager.extensions.makePublicKey
import com.tangem.data.walletmanager.extensions.makeWalletManagerForApp
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

    /** A provider of the balance of [address] in [blockchain] — no key, no derivation; `null` if the SDK cannot serve it */
    suspend fun createExternalAddressBalanceProvider(
        blockchain: Blockchain,
        address: String,
    ): ExternalAddressBalanceProvider? {
        return try {
            blockchainSDKFactory.getWalletManagerFactorySync()
                ?.createExternalAddressBalanceProvider(blockchain = blockchain, address = address)
        } catch (e: Throwable) {
            TangemLogger.e("Failed to create external address balance provider for $blockchain", e)
            null
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