package com.tangem.lib.wallet.impl

import com.tangem.lib.wallet.api.WalletManager
import com.tangem.lib.wallet.api.WalletStore
import com.tangem.lib.wallet.impl.managers.WalletManagersFactory
import com.tangem.lib.wallet.impl.scan.ScanProcessor

class WalletStoreImpl(private val scanProcessor: ScanProcessor) : WalletStore {

    private val walletManagerFactory = WalletManagersFactory()

    override suspend fun createWallet(): WalletManager {
        return walletManagerFactory.createWalletManagerFromScanResponse(
            scanProcessor.scan(),
        )
    }
}