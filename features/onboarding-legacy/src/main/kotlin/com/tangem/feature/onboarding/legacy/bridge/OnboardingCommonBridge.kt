package com.tangem.feature.onboarding.legacy.bridge

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

interface OnboardingCommonBridge {

    fun trySaveWalletAndNavigateToWalletScreen(scanResponse: ScanResponse)

    fun handleTopUpAction(walletManager: WalletManager, scanResponse: ScanResponse)

    fun tryHandleDemoCard(action: Action): Boolean

    fun sendToppedUpAnalyticEvent(scanResponse: ScanResponse)
}
