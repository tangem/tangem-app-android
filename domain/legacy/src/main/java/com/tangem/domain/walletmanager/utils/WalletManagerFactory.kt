package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.makeWalletManagerForApp
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse

internal class WalletManagerFactory(
    private val configManager: ConfigManager,
) {

    private val sdkWalletManagerFactory by lazy {
        WalletManagerFactory(configManager.config.blockchainSdkConfig)
    }

    fun createWalletManager(
        scanResponse: ScanResponse,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
    ): WalletManager? {
        val derivationParams = getDerivationParams(derivationPath, scanResponse.derivationStyleProvider)

        return sdkWalletManagerFactory.makeWalletManagerForApp(
            scanResponse = scanResponse,
            blockchain = blockchain,
            derivationParams = derivationParams,
        )
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
