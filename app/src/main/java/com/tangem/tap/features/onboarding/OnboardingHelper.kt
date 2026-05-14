package com.tangem.tap.features.onboarding

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.features.demo.DemoHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingHelper @Inject constructor(
    private val cardRepository: CardRepository,
) {
    suspend fun isOnboardingCase(response: ScanResponse): Boolean {
        val cardId = response.card.cardId

        return when {
            response.cardTypesResolver.isVisaWallet() -> {
                // if (response.visaCardActivationStatus == null) error("Visa card activation status is null")
                //
                // response.visaCardActivationStatus !is VisaCardActivationStatus.Activated
                true
            }

            response.cardTypesResolver.isTangemTwins() -> {
                if (!response.twinsIsTwinned()) {
                    true
                } else {
                    cardRepository.isActivationInProgress(cardId)
                }
            }

            response.cardTypesResolver.isWallet2() || response.cardTypesResolver.isShibaWallet() -> {
                val areWalletsEmpty = response.card.wallets.isEmpty()
                val isActivationInProgress = cardRepository.isActivationInProgress(cardId)
                val isNoBackup = response.card.backupStatus == CardDTO.BackupStatus.NoBackup &&
                    !DemoHelper.isDemoCard(response)
                areWalletsEmpty || isActivationInProgress || isNoBackup
            }

            response.card.wallets.isNotEmpty() -> cardRepository.isActivationInProgress(cardId)
            else -> true
        }
    }
}