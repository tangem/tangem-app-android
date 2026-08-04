package com.tangem.features.onboarding.v2.multiwallet.impl.common

import com.tangem.domain.models.scan.CardDTO
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

     */
    fun reportWalletCreated(scanResponse: ScanResponse, usedSeed: Boolean) {
        report(scanResponse = scanResponse, backupCards = emptyList(), usedSeed = usedSeed)
    }

    /**
     * Reports the primary card together with every backup card added so far, called each time a backup card is
     * added on the Creating a backup screen.
     *
     * Only cards the backup service accepted reach this point, which is what makes them eligible for backup.
     *
     * @param backupCards added backup cards, in the order they were added
     */
    fun reportBackupCardAdded(scanResponse: ScanResponse, backupCards: List<CardDTO>) {
        report(
            scanResponse = scanResponse,
            backupCards = backupCards,
            // unlike wallet creation, this step cannot know it firsthand — the wallet may have been created in an
            // earlier session — so it is read off the card: an imported wallet is one created from a seed phrase
            usedSeed = scanResponse.card.wallets.any { it.isImported },
        )
    }

    private fun report(scanResponse: ScanResponse, backupCards: List<CardDTO>, usedSeed: Boolean) {
        if (onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled.not()) return

        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

        if (userWalletId == null) {
            TangemLogger.e("Unable to build user wallet id, cards backup state is not reported")
            return
        }

        if (backupCards.size > BACKUP_ROLES.size) {
            TangemLogger.e("Got ${backupCards.size} backup cards, only the first ${BACKUP_ROLES.size} are reported")
        }

        val primaryCard = CardBackupConverter.convert(
            card = scanResponse.card,
            role = WalletCardBackup.Role.PRIMARY,
        )
        val cards = listOf(primaryCard) + backupCards.zip(BACKUP_ROLES) { card, role ->
            CardBackupConverter.convert(card = card, role = role)
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

    private companion object {
        val BACKUP_ROLES = listOf(WalletCardBackup.Role.BACKUP_1, WalletCardBackup.Role.BACKUP_2)
    }
}