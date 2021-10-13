package com.tangem.tap.features.onboarding.service

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.preferencesStorage

/**
[REDACTED_AUTHOR]
 */
class OnboardingHelper {
    companion object {

        fun isOnboardingCase(response: ScanResponse): Boolean = if (response.card.hasWallets())
            preferencesStorage.usedCardsPrefStorage.activationIsStarted(response.card.cardId) else true

        fun createService(
            fromScreen: AppScreen,
            scanResponse: ScanResponse,
            walletManagerFactory: WalletManagerFactory,
        ): ProductOnboardingService? {
            val cardInfoStorage = preferencesStorage.usedCardsPrefStorage
            return when(scanResponse.productType) {
                ProductType.Note -> OnboardingNoteService(fromScreen, scanResponse, cardInfoStorage, walletManagerFactory)
                ProductType.Wallet -> null
                ProductType.Twin -> null
                ProductType.Other -> OnboardingOtherCardsService(fromScreen, scanResponse, cardInfoStorage, walletManagerFactory)
            }
        }
    }
}