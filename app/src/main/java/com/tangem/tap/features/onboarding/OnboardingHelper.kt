package com.tangem.tap.features.onboarding

import com.tangem.blockchain.common.WalletManager
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletAction
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.mainScope
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
object OnboardingHelper {
    suspend fun isOnboardingCase(response: ScanResponse): Boolean {
        val onboardingManager =
            store.state.globalState.onboardingState.onboardingManager ?: OnboardingManager(response)
        val cardId = response.card.cardId
        return when {
            response.cardTypesResolver.isTangemTwins() -> {
                if (!response.twinsIsTwinned()) {
                    true
                } else {
                    onboardingManager.isActivationInProgress(cardId) ?: false
                }
            }

            response.cardTypesResolver.isWallet2() || response.cardTypesResolver.isShibaWallet() -> {
                val emptyWallets = response.card.wallets.isEmpty()
                val activationInProgress = onboardingManager.isActivationInProgress(cardId)
                val isNoBackup = response.card.backupStatus == CardDTO.BackupStatus.NoBackup &&
                    !DemoHelper.isDemoCard(response)
                emptyWallets || activationInProgress || isNoBackup
            }

            response.card.wallets.isNotEmpty() -> onboardingManager.isActivationInProgress(cardId) ?: false
            else -> true
        }
    }

    fun whereToNavigate(scanResponse: ScanResponse): AppRoute {
        if (store.inject(DaggerGraphState::onboardingV2FeatureToggles).isOnboardingV2Enabled) {
            return AppRoute.Onboarding(
                scanResponse = scanResponse,
                startFromBackup = false,
            )
        }

        return when (val type = scanResponse.productType) {
            ProductType.Note -> AppRoute.OnboardingNote
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Ring,
            -> if (scanResponse.card.settings.isBackupAllowed) {
                AppRoute.OnboardingWallet()
            } else {
                AppRoute.OnboardingOther
            }
            ProductType.Twins -> AppRoute.OnboardingTwins
            ProductType.Start2Coin,
            ProductType.Visa,
            -> throw UnsupportedOperationException("Onboarding for ${type.name} cards is not supported")
        }
    }

    fun saveWallet(
        alreadyCreatedWallet: UserWallet?,
        scanResponse: ScanResponse,
        accessCode: String? = null,
        backupCardsIds: List<String>? = null,
        hasBackupError: Boolean = false,
    ) {
        Analytics.setContext(scanResponse)
        scope.launch {
            val settingsRepository = store.inject(DaggerGraphState::settingsRepository)
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

                    store.dispatchWithMain(
                        SaveWalletAction.SaveWalletAfterBackup(
                            hasBackupError = hasBackupError,
                            shouldNavigateToWallet = false,
                        ),
                    )
                }
                // When should not save user wallets but device has biometry and save wallet screen has not been shown,
                // then open save wallet screen
                tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowSaveUserWalletScreen() -> {
                    proceedWithScanResponse(scanResponse, backupCardsIds, hasBackupError, alreadyCreatedWallet)

                    delay(timeMillis = 1_200)

                    store.dispatchOnMain(
                        SaveWalletAction.ProvideBackupInfo(
                            scanResponse = scanResponse,
                            accessCode = accessCode,
                            backupCardsIds = backupCardsIds?.toSet(),
                        ),
                    )
                }
                // If device has no biometry and save wallet screen has been shown, then go through old scenario
                else -> {
                    proceedWithScanResponse(scanResponse, backupCardsIds, hasBackupError, alreadyCreatedWallet)
                }
            }
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
            val settingsRepository = store.inject(DaggerGraphState::settingsRepository)

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

                    store.dispatchWithMain(
                        SaveWalletAction.SaveWalletAfterBackup(
                            hasBackupError = hasBackupError,
                            shouldNavigateToWallet = true,
                        ),
                    )
                }
                // When should not save user wallets but device has biometry and save wallet screen has not been shown,
                // then open save wallet screen
                tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowSaveUserWalletScreen() -> {
                    proceedWithScanResponse(scanResponse, backupCardsIds, hasBackupError)

                    delay(timeMillis = 1_200)

                    store.dispatchOnMain(
                        SaveWalletAction.ProvideBackupInfo(
                            scanResponse = scanResponse,
                            accessCode = accessCode,
                            backupCardsIds = backupCardsIds?.toSet(),
                        ),
                    )
                    store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
                    delay(timeMillis = 1_800)
                    store.dispatchNavigationAction { push(AppRoute.SaveWallet) }
                }
                // If device has no biometry and save wallet screen has been shown, then go through old scenario
                else -> {
                    proceedWithScanResponse(scanResponse, backupCardsIds, hasBackupError)
                    store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
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

    fun handleTopUpAction(walletManager: WalletManager, scanResponse: ScanResponse, globalState: GlobalState) {
        val blockchain = walletManager.wallet.blockchain
        val excludedBlockchains = store.inject(DaggerGraphState::excludedBlockchains)

        val cryptoCurrency = CryptoCurrencyFactory(excludedBlockchains).createCoin(
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = scanResponse,
        ) ?: return

        val topUpUrl = walletManager.getTopUpUrl(cryptoCurrency) ?: return

        val currencyType = AnalyticsParam.CurrencyType.Blockchain(blockchain)
        Analytics.send(Onboarding.Topup.ButtonBuyCrypto(currencyType))

        scope.launch {
            val homeFeatureToggles = store.inject(DaggerGraphState::homeFeatureToggles)

            val isRussia = if (homeFeatureToggles.isMigrateUserCountryCodeEnabled) {
                val getUserCountryCodeUseCase = store.inject(DaggerGraphState::getUserCountryUseCase)

                getUserCountryCodeUseCase().isRight { it is UserCountry.Russia }
            } else {
                globalState.userCountryCode == RUSSIA_COUNTRY_CODE
            }

            if (isRussia) {
                val dialogData = AppDialog.RussianCardholdersWarningDialog.Data(topUpUrl)
                store.dispatchDialogShow(AppDialog.RussianCardholdersWarningDialog(dialogData))
            } else {
                store.dispatchOpenUrl(topUpUrl)
            }
        }
    }

    private suspend fun proceedWithScanResponse(
        scanResponse: ScanResponse,
        backupCardsIds: List<String>?,
        hasBackupError: Boolean,
        alreadyCreatedWallet: UserWallet? = null,
    ) {
        val walletNameGenerateUseCase = store.inject(DaggerGraphState::generateWalletNameUseCase)
        val userWallet = alreadyCreatedWallet ?: UserWalletBuilder(scanResponse, walletNameGenerateUseCase)
            .hasBackupError(hasBackupError)
            .backupCardsIds(backupCardsIds?.toSet())
            .build()
            .guard {
                Timber.e("User wallet not created")
                return
            }

        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
        userWalletsListManager.save(userWallet, canOverride = true)
            .doOnFailure { error ->
                Timber.e(error, "Unable to save user wallet")
            }
            .doOnSuccess {
                mainScope.launch { store.onUserWalletSelected(userWallet) }
                store.dispatch(OnboardingWalletAction.WalletSaved(userWallet.walletId))
            }
    }
}
