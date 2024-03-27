package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.makeWalletManagerForApp
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import timber.log.Timber
import com.tangem.blockchain.common.WalletManagerFactory as BlockchainWalletManagerFactory

internal class WalletManagerFactory(
    configManager: ConfigManager,
    accountCreator: AccountCreator,
    blockchainDataStorage: BlockchainDataStorage,
    blockchainSDKLogger: BlockchainSDKLogger? = null,
) {

    private val sdkWalletManagerFactory by lazy {
        BlockchainWalletManagerFactory(
            config = configManager.config.blockchainSdkConfig,
            accountCreator = accountCreator,
            blockchainDataStorage = blockchainDataStorage,
            loggers = listOfNotNull(blockchainSDKLogger),
        )
    }

    fun createWalletManager(
        scanResponse: ScanResponse,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
    ): WalletManager? {
        val derivationParams = getDerivationParams(derivationPath, scanResponse.derivationStyleProvider)

        return try {
            sdkWalletManagerFactory.makeWalletManagerForApp(
                scanResponse = scanResponse,
                blockchain = blockchain,
                derivationParams = derivationParams,
            )
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