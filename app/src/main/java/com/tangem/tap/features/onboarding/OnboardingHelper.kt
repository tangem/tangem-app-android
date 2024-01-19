package com.tangem.tap.features.onboarding

import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.tap.*
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.core.analytics.models.Basic
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.extensions.removeContext
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
[REDACTED_AUTHOR]
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

            response.cardTypesResolver.isWallet2() || response.cardTypesResolver.isShibaWallet() -> {
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
        return when (val type = scanResponse.productType) {
            ProductType.Note -> AppScreen.OnboardingNote
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Ring,
            -> if (scanResponse.card.settings.isBackupAllowed) {
                AppScreen.OnboardingWallet
            } else {
                AppScreen.OnboardingOther
            }
            ProductType.Twins -> AppScreen.OnboardingTwins
            ProductType.Start2Coin,
            ProductType.Visa,
            -> throw UnsupportedOperationException("Onboarding for ${type.name} cards is not supported")
        }
    }

    fun trySaveWalletAndNavigateToWalletScreen(
        scanResponse: ScanResponse,
        accessCode: String? = null,
        backupCardsIds: List<String>? = null,
    ) {
        Analytics.setContext(scanResponse)
        scope.launch {
            when {
                // When should save user wallets, then save card without navigate to save wallet screen
                store.state.daggerGraphState.get(DaggerGraphState::walletsRepository).shouldSaveUserWalletsSync() -> {
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
                tangemSdkManager.canUseBiometry && preferencesStorage.shouldShowSaveUserWalletScreen -> {
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
                else -> proceedWithScanResponse(scanResponse, backupCardsIds)
            }
        }

        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
    }

    fun onInterrupted() {
        Analytics.removeContext()
    }

    fun sendToppedUpEvent(scanResponse: ScanResponse) {
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
        val currency = ParamCardCurrencyConverter().convert(scanResponse.cardTypesResolver)

        if (userWalletId != null && currency != null) {
            Analytics.send(Basic.ToppedUp(userWalletId.stringValue, currency))
        }
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