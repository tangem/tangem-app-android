package com.tangem.feature.onboarding.legacy.bridge

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.services.Result

interface WalletManagerBridge {
    fun WalletManager.safeUpdate(idDemoCard: Boolean): Result<Wallet>
}
