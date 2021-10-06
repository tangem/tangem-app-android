package com.tangem.tap.features.onboarding.service

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.preferencesStorage

/**
[REDACTED_AUTHOR]
 */
class OnboardingHelper {
    companion object {

        fun isOnboardingCase(response: ScanNoteResponse): Boolean = if (response.card.hasWallets())
            preferencesStorage.usedCardsPrefStorage.activationIsStarted(response.card.cardId) else true

        fun createService(
            fromScreen: AppScreen,
            scanResponse: ScanNoteResponse,
            walletManagerFactory: WalletManagerFactory,
        ): ProductOnboardingService? {
            val card = scanResponse.card
            val cardInfoStorage = preferencesStorage.usedCardsPrefStorage
            return when {
                card.isTangemNote() -> OnboardingNoteService(fromScreen, scanResponse, cardInfoStorage,
                        walletManagerFactory)
                else -> null
            }
        }
    }
}