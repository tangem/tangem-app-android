package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.extensions.makeWalletManagerForApp
import com.tangem.domain.models.scan.CardDTO
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
        val derivationParams = getDerivationParams(derivationPath, scanResponse.card)

        return sdkWalletManagerFactory.makeWalletManagerForApp(
            scanResponse = scanResponse,
            blockchain = blockchain,
            derivationParams = derivationParams,
        )
    }

    private fun getDerivationParams(derivationPath: DerivationPath?, card: CardDTO): DerivationParams? {
        val derivationStyle = when {
            !card.settings.isHDWalletAllowed -> return null
            card.useOldStyleDerivation -> DerivationStyle.LEGACY
            else -> DerivationStyle.NEW
        }

        return if (derivationPath == null) {
            DerivationParams.Default(derivationStyle)
        } else {
            DerivationParams.Custom(derivationPath)
        }
    }
}