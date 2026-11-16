package com.tangem.tap.domain.walletcardsbackup

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.backup.SeenWalletCardReporter
import com.tangem.domain.wallets.usecase.ReportSeenWalletCardUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class DefaultSeenWalletCardReporter @Inject constructor(
    private val reportSeenWalletCardUseCase: ReportSeenWalletCardUseCase,
    private val appCoroutineScope: AppCoroutineScope,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
) : SeenWalletCardReporter {

    override fun report(userWallet: UserWallet.Cold, isNewWallet: Boolean) {
        if (!onboardingV2FeatureToggles.isCardLinkedStatusUpdateV2Enabled) return

        appCoroutineScope.launch {
            reportSeenWalletCardUseCase(userWallet = userWallet, isNewWallet = isNewWallet)
                .onLeft { TangemLogger.e("Unable to report a seen card of wallet=${userWallet.walletId}: $it") }
        }
    }
}