package com.tangem.data.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.walletmanager.extensions.makePublicKey
import com.tangem.data.walletmanager.extensions.makeWalletManagerForApp
import com.tangem.domain.card.DerivationStyleProvider
import com.tangem.domain.card.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import timber.log.Timber

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
            Timber.w(e, "Failed to create wallet manager for $blockchain")
            null
        }
    }

    suspend fun createWalletManagerForHot(
        hotWallet: UserWallet.Hot,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
    ): WalletManager? {
        val curve = blockchain.getSupportedCurves().first()
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
            Timber.w(e, "Failed to create wallet manager for $blockchain")
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