package com.tangem.features.onboarding.v2.multiwallet.impl.common

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.backup.CardBackupConverter
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.usecase.ReportWalletCardsBackupUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reports the cards of a wallet and the state of their backup to the backend during onboarding.
 */
internal class WalletCardsBackupReporter @Inject constructor(
    private val reportWalletCardsBackupUseCase: ReportWalletCardsBackupUseCase,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    private val appScope: AppCoroutineScope,
) {

    /**

     * backup has started yet.
     *
     * @param scanResponse scan response updated with the card the create wallet command returned

     */
    fun reportWalletCreated(scanResponse: ScanResponse, usedSeed: Boolean) {
        if (onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled.not()) return

        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

        if (userWalletId == null) {
            TangemLogger.e("Unable to build user wallet id, primary card backup state is not reported")
            return
        }

        val primaryCard = CardBackupConverter.convert(
            card = scanResponse.card,
            role = WalletCardBackup.Role.PRIMARY,
        )

        // deliberately not modelScope: the create wallet step navigates away as soon as the wallet is created,
        // which destroys the model and would cancel this request mid-flight — the backend would then never learn
        // about a card that already holds a wallet, exactly the state this reporting exists to make visible
        appScope.launch {
            reportWalletCardsBackupUseCase(
                userWalletId = userWalletId,
                cards = listOf(primaryCard),
                usedSeed = usedSeed,
            ).onLeft { error ->
                TangemLogger.e("Unable to report primary card backup state: $error")
            }
        }
    }
}