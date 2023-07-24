package com.tangem.tap.features.onboarding

import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.TapWorkarounds.canSkipBackup
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.extensions.removeContext
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
object OnboardingHelper {
    fun isOnboardingCase(response: ScanResponse): Boolean {
        val cardInfoStorage = preferencesStorage.usedCardsPrefStorage
        val cardId = response.card.cardId
        return when {
            response.cardTypesResolver.isTangemTwins() -> {
                if (!response.twinsIsTwinned()) {
                    true
                } else {
                    cardInfoStorage.isActivationInProgress(cardId)
                }
            }
// [REDACTED_TODO_COMMENT]
            //  (AND-4057)
            // response.cardTypesResolver.isWallet2() -> {
            !response.card.canSkipBackup -> {
                val emptyWallets = response.card.wallets.isEmpty()
                val activationInProgress = cardInfoStorage.isActivationInProgress(cardId)
                val backupNotActive = response.card.backupStatus?.isActive != true
                emptyWallets || activationInProgress || backupNotActive
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
        Analytics.setContext(scanResponse)
        when {
            // When should save user wallets, then save card without navigate to save wallet screen
            preferencesStorage.shouldSaveUserWallets -> scope.launch {
                proceedWithScanResponse(scanResponse, backupCardsIds)

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
                proceedWithScanResponse(scanResponse, backupCardsIds)

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
                proceedWithScanResponse(scanResponse, backupCardsIds)
            }
        }

        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
    }

    fun onInterrupted() {
        Analytics.removeContext()
    }

    private suspend fun proceedWithScanResponse(scanResponse: ScanResponse, backupCardsIds: List<String>?) {
        val userWallet = UserWalletBuilder(scanResponse)
            .backupCardsIds(backupCardsIds?.toSet())
            .build()
            .guard {
                Timber.e("User wallet not created")
                return
            }

        userWalletsListManager.save(userWallet, canOverride = true)
            .doOnFailure { error ->
                Timber.e(error, "Unable to save user wallet")
            }
            .doOnSuccess {
                scope.launch { store.onUserWalletSelected(userWallet) }
            }
    }
}
