package com.tangem.tap.features.onboarding

import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.tap.*
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.extensions.*
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 05/10/2021.
 */
object OnboardingHelper {
    suspend fun isOnboardingCase(response: ScanResponse): Boolean {
        val onboardingManager = store.state.globalState.onboardingState.onboardingManager
        val cardId = response.card.cardId
        return when {
            response.cardTypesResolver.isTangemTwins() -> {
                if (!response.twinsIsTwinned()) {
                    true
                } else {
                    onboardingManager?.isActivationInProgress(cardId) ?: false
                }
            }

            response.cardTypesResolver.isWallet2() || response.cardTypesResolver.isShibaWallet() -> {
                val emptyWallets = response.card.wallets.isEmpty()
                val activationInProgress = onboardingManager?.isActivationInProgress(cardId)
                val isNoBackup = response.card.backupStatus == CardDTO.BackupStatus.NoBackup
                emptyWallets || activationInProgress == true || isNoBackup
            }

            response.card.wallets.isNotEmpty() -> onboardingManager?.isActivationInProgress(cardId) ?: false
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
        hasBackupError: Boolean = false,
    ) {
        Analytics.setContext(scanResponse)
        scope.launch {
            when {
                // When should save user wallets, then save card without navigate to save wallet screen
                store.inject(DaggerGraphState::walletsRepository).shouldSaveUserWalletsSync() -> {
                    store.dispatchWithMain(
                        SaveWalletAction.ProvideBackupInfo(
                            scanResponse = scanResponse,
                            accessCode = accessCode,
                            backupCardsIds = backupCardsIds?.toSet(),
                        ),
                    )

                    val toggles = store.inject(DaggerGraphState::userWalletsListManagerFeatureToggles)
                    if (toggles.isGeneralManagerEnabled) {
                        store.dispatchWithMain(SaveWalletAction.SaveWalletAfterBackup(hasBackupError))
                    } else {
                        store.dispatchWithMain(SaveWalletAction.Save)
                    }
                }
                // When should not save user wallets but device has biometry and save wallet screen has not been shown,
                // then open save wallet screen
                tangemSdkManager.canUseBiometry && preferencesStorage.shouldShowSaveUserWalletScreen -> {
                    proceedWithScanResponse(scanResponse, backupCardsIds, hasBackupError)

                    delay(timeMillis = 1_200)

                    store.dispatchOnMain(
                        SaveWalletAction.ProvideBackupInfo(
                            scanResponse = scanResponse,
                            accessCode = accessCode,
                            backupCardsIds = backupCardsIds?.toSet(),
                        ),
                    )
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                    delay(timeMillis = 1_800)
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.SaveWallet))
                }
                // If device has no biometry and save wallet screen has been shown, then go through old scenario
                else -> {
                    proceedWithScanResponse(scanResponse, backupCardsIds, hasBackupError)
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                }
            }
        }
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

    private suspend fun proceedWithScanResponse(
        scanResponse: ScanResponse,
        backupCardsIds: List<String>?,
        hasBackupError: Boolean,
    ) {
        val userWallet = UserWalletBuilder(scanResponse = scanResponse)
            .hasBackupError(hasBackupError)
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
                mainScope.launch { store.onUserWalletSelected(userWallet) }
            }
    }
}
