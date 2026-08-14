package com.tangem.features.onboarding.v2.multiwallet.impl.common

import com.tangem.domain.models.scan.ScanResponse
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
     * Reports every card known to the app for this wallet, called on each step that changes what is known —

     *
     * @param scanResponse scan response holding the primary card, the source of the wallet id
     * @param cards        all known cards, primary first

     *                     wallet creation take it from [usedSeedPhrase]
     */
    fun report(scanResponse: ScanResponse, cards: List<WalletCardBackup>, usedSeed: Boolean) {
        if (onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled.not()) return

        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

        if (userWalletId == null) {
            TangemLogger.e("Unable to build user wallet id, cards backup state is not reported")
            return
        }

        // deliberately not modelScope: an onboarding step navigates away right after it reports, which destroys the
        // model and would cancel the request mid-flight — the backend would then never learn about a card that is
        // already linked to a wallet, exactly the state this reporting exists to make visible
        appScope.launch {
            reportWalletCardsBackupUseCase(
                userWalletId = userWalletId,
                cards = cards,
                usedSeed = usedSeed,
            ).onLeft { error ->
                TangemLogger.e("Unable to report cards backup state: $error")
            }
        }
    }

    internal companion object {

        /** Roles of the backup cards, in the order the cards are added to the backup */
        val BACKUP_ROLES = listOf(WalletCardBackup.Role.BACKUP_1, WalletCardBackup.Role.BACKUP_2)
    }
}

/**

 *
 * Only wallet creation knows this firsthand; every later step — a backup card is added, a card is finalized — may

 */
internal fun ScanResponse.usedSeedPhrase(): Boolean = card.wallets.any { it.isImported }