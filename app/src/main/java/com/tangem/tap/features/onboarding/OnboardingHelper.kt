package com.tangem.tap.features.onboarding

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
object OnboardingHelper {
    suspend fun isOnboardingCase(response: ScanResponse): Boolean {
        val cardRepository = store.inject(DaggerGraphState::cardRepository)
        val cardId = response.card.cardId

        return when {
            response.cardTypesResolver.isVisaWallet() -> {
                if (response.visaCardActivationStatus == null) error("Visa card activation status is null")

                response.visaCardActivationStatus !is VisaCardActivationStatus.Activated
            }

            response.cardTypesResolver.isTangemTwins() -> {
                if (!response.twinsIsTwinned()) {
                    true
                } else {
                    cardRepository.isActivationInProgress(cardId)
                }
            }

            response.cardTypesResolver.isWallet2() || response.cardTypesResolver.isShibaWallet() -> {
                val emptyWallets = response.card.wallets.isEmpty()
                val activationInProgress = cardRepository.isActivationInProgress(cardId)
                val isNoBackup = response.card.backupStatus == CardDTO.BackupStatus.NoBackup &&
                    !DemoHelper.isDemoCard(response)
                emptyWallets || activationInProgress || isNoBackup
            }

            response.card.wallets.isNotEmpty() -> cardRepository.isActivationInProgress(cardId)
            else -> true
        }
    }
}