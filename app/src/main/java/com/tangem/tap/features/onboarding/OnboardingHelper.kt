package com.tangem.tap.features.onboarding

import com.tangem.domain.common.ProductType
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
* [REDACTED_AUTHOR]
 */
object OnboardingHelper {
    fun isOnboardingCase(response: ScanResponse): Boolean {
        val cardInfoStorage = preferencesStorage.usedCardsPrefStorage
        val cardId = response.card.cardId
        return when {
            response.isTangemTwins() -> {
                if (!response.twinsIsTwinned()) {
                    true
                } else {
                    cardInfoStorage.isActivationInProgress(cardId)
                }
            }
            response.card.wallets.isNotEmpty() -> cardInfoStorage.isActivationInProgress(cardId)
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
            ProductType.Start2Coin -> throw java.lang.UnsupportedOperationException(
                "Onboarding for Start2Coin cards is not supported",
            )
        }
    }

    fun trySaveWalletAndNavigateToWalletScreen(
        scanResponse: ScanResponse,
        accessCode: String? = null,
        backupCardsIds: List<String>? = null,
    ) {
        when {
            // When should save user wallets, then save card without navigate to save wallet screen
            preferencesStorage.shouldSaveUserWallets -> scope.launch {
                store.dispatchOnMain(
                    SaveWalletAction.ProvideBackupInfo(
                        scanResponse = scanResponse,
                        accessCode = accessCode,
                        backupCardsIds = backupCardsIds?.toSet(),
                    ),
                )
                store.dispatchOnMain(SaveWalletAction.Save)
            }
            // When should not save user wallets but device has biometry and save wallet screen has not been shown,
            // then open save wallet screen
            tangemSdkManager.canUseBiometry &&
                preferencesStorage.shouldShowSaveUserWalletScreen -> scope.launch {
                store.onCardScanned(scanResponse)

                delay(timeMillis = 1_200)

                store.dispatchOnMain(
                    SaveWalletAction.ProvideBackupInfo(
                        scanResponse = scanResponse,
                        accessCode = accessCode,
                        backupCardsIds = backupCardsIds?.toSet(),
                    ),
                )
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.SaveWallet))
            }
            // If device has no biometry and save wallet screen has been shown, then go through old scenario
            else -> scope.launch {
                store.onCardScanned(scanResponse)
            }
        }

        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
    }
}
