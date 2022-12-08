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
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
* [REDACTED_AUTHOR]
 */
class OnboardingHelper {
    companion object {

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
            }
        }

        fun trySaveWalletAndNavigateToWalletScreen(
            scanResponse: ScanResponse,
            accessCode: String? = null,
            backupCardsIds: List<String>? = null,
        ) {
            when {
                userWalletsListManager.hasSavedUserWallets -> scope.launch {
                    delay(timeMillis = 1_200)
                    store.dispatchOnMain(
                        SaveWalletAction.ProvideBackupInfo(
                            scanResponse = scanResponse,
                            accessCode = accessCode,
                            backupCardsIds = backupCardsIds?.toSet(),
                        ),
                    )
                    store.dispatchOnMain(SaveWalletAction.Save)
                }
                tangemSdkManager.canUseBiometry &&
                    preferencesStorage.shouldShowSaveWallet -> scope.launch {
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
                else -> scope.launch {
                    store.onCardScanned(scanResponse)
                }
            }

            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
        }
    }
}
