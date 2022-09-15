package com.tangem.tap.features.onboarding

import com.tangem.domain.common.ProductType
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.preferencesStorage

/**
[REDACTED_AUTHOR]
 */
class OnboardingHelper {
    companion object {

        fun isOnboardingCase(response: ScanResponse): Boolean {
            val cardInfoStorage = preferencesStorage.usedCardsPrefStorage
            return when {
                response.card.isSaltPay -> {
                    // response.card.backupStatus?.isActive != true //TODO: restore after presentation
                    false
                }
                response.productType == ProductType.Twins -> {
                    if (!response.twinsIsTwinned()) {
                        true
                    } else {
                        cardInfoStorage.activationIsStarted(response.card.cardId)
                    }
                }
                response.card.hasWallets() -> {
                    cardInfoStorage.activationIsStarted(response.card.cardId)
                }
                else -> true
            }
        }

        fun whereToNavigate(scanResponse: ScanResponse): AppScreen {
            return when (scanResponse.productType) {
                ProductType.Note -> AppScreen.OnboardingNote
                ProductType.Wallet -> if (scanResponse.card.settings.isBackupAllowed) {
                    AppScreen.OnboardingWallet
                } else {
                    AppScreen.OnboardingOther
                }
                ProductType.Twins -> AppScreen.OnboardingTwins
                ProductType.SaltPay -> AppScreen.OnboardingWallet
            }
        }
    }
}