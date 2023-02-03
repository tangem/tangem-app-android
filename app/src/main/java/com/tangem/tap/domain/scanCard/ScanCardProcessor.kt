package com.tangem.tap.domain.scanCard

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.backup.BackupService
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.backupService
import com.tangem.tap.common.analytics.paramsInterceptor.BatchIdParamsInterceptor
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.primaryCardIsSaltPayVisa
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.disclaimer.DisclaimerType
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerCallback
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.OnboardingSaltPayHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayExceptionHandler
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userTokensRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Create repository for that
object ScanCardProcessor {
    suspend fun scan(
        analyticsEvent: AnalyticsEvent? = null,
        additionalBlockchainsToDerive: Collection<Blockchain>? = null,
        cardId: String? = null,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit = {},
        onScanStateChange: suspend (scanInProgress: Boolean) -> Unit = {},
        onWalletNotCreated: suspend () -> Unit = {},
        disclaimerWillShow: () -> Unit = {},
        onFailure: suspend (error: TangemError) -> Unit = {},
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit = {},
    ) = withMainContext {
        onProgressStateChange(true)
        onScanStateChange(true)

        tangemSdkManager.changeDisplayedCardIdNumbersCount(null)

        val result = tangemSdkManager.scanProduct(
            userTokensRepository = userTokensRepository,
            cardId = cardId,
            additionalBlockchainsToDerive = additionalBlockchainsToDerive,
        )

        store.dispatchOnMain(GlobalAction.ScanFailsCounter.ChooseBehavior(result))

        result
            .doOnFailure { error ->
                onScanStateChange(false)
                onFailure(error)
            }
            .doOnSuccess { scanResponse ->
                tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

                onScanStateChange(false)
                sendAnalytics(analyticsEvent, scanResponse.card.batchId)

                checkForUnfinishedBackupForSaltPay(
                    backupService = backupService,
                    scanResponse = scanResponse,
                    onFailure = onFailure,
                    nextHandler = { scanResponse1 ->
                        showDisclaimerIfNeed(
                            scanResponse = scanResponse1,
                            disclaimerWillShow = disclaimerWillShow,
                            onFailure = onFailure,
                            nextHandler = { scanResponse2 ->
                                onScanSuccess(
                                    scanResponse = scanResponse2,
                                    onProgressStateChange = onProgressStateChange,
                                    onSuccess = onSuccess,
                                    onWalletNotCreated = onWalletNotCreated,
                                    onFailure = onFailure,
                                )
                            },
                        )
                    },
                )
            }
    }

    private fun sendAnalytics(
        analyticsEvent: AnalyticsEvent?,
        batchId: String,
    ) {
        Analytics.addParamsInterceptor(BatchIdParamsInterceptor(batchId))
        analyticsEvent?.let { Analytics.send(it) }
    }

    /**
     * It checks only the SaltPay cards. To check for unfinished backups for the standard Wallet cards
     * see BackupAction.CheckForUnfinishedBackup
     * If user touches card other than Visa SaltPay - show dialog and block next processing
     */
    private suspend inline fun checkForUnfinishedBackupForSaltPay(
        backupService: BackupService,
        scanResponse: ScanResponse,
        nextHandler: (ScanResponse) -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
    ) {
        if (!backupService.hasIncompletedBackup || !backupService.primaryCardIsSaltPayVisa()) {
            nextHandler(scanResponse)
            return
        }

        val isTheSamePrimaryCard = backupService.primaryCardId
            ?.let { it == scanResponse.card.cardId }
            ?: false

        if (scanResponse.cardTypesResolver.isSaltPayWallet() || !isTheSamePrimaryCard) {
            val error = SaltPayActivationError.PutVisaCard
            SaltPayExceptionHandler.handle(error)
            onFailure(TangemSdkError.ExceptionError(error))
        } else {
            nextHandler(scanResponse)
        }
    }

    private suspend inline fun showDisclaimerIfNeed(
        scanResponse: ScanResponse,
        crossinline disclaimerWillShow: () -> Unit = {},
        crossinline nextHandler: suspend (ScanResponse) -> Unit,
        crossinline onFailure: suspend (error: TangemError) -> Unit,
    ) {
        val disclaimer = DisclaimerType.get(scanResponse.card).createDisclaimer(scanResponse.card)
        store.dispatchOnMain(DisclaimerAction.SetDisclaimer(disclaimer))

        if (disclaimer.isAccepted()) {
            nextHandler(scanResponse)
        } else {
            scope.launch {
                delay(DELAY_SDK_DIALOG_CLOSE)
                disclaimerWillShow()
                dispatchOnMain(
                    DisclaimerAction.Show(
                        fromScreen = AppScreen.Home,
                        callback = DisclaimerCallback(
                            onAccept = {
                                scope.launch(Dispatchers.Main) {
                                    nextHandler(scanResponse)
                                }
                            },
                            onDismiss = {
                                scope.launch(Dispatchers.Main) {
                                    onFailure(TangemSdkError.UserCancelled())
                                }
                            },
                        ),
                    ),
                )
            }
        }
    }

    @Suppress("LongMethod", "MagicNumber")
    private suspend inline fun onScanSuccess(
        scanResponse: ScanResponse,
        crossinline onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        crossinline onWalletNotCreated: suspend () -> Unit,
        crossinline onSuccess: suspend (ScanResponse) -> Unit,
        crossinline onFailure: suspend (error: TangemError) -> Unit,
    ) {
        val globalState = store.state.globalState
        val tapWalletManager = globalState.tapWalletManager
        tapWalletManager.updateConfigManager(scanResponse)

        store.dispatchOnMain(TwinCardsAction.IfTwinsPrepareState(scanResponse))

        if (scanResponse.cardTypesResolver.isSaltPay()) {
            if (scanResponse.cardTypesResolver.isSaltPayVisa()) {
                val (manager, config) = OnboardingSaltPayState.initDependency(scanResponse)
                val result = OnboardingSaltPayHelper.isOnboardingCase(scanResponse, manager)
                delay(500)
                withMainContext {
                    when (result) {
                        is Result.Success -> {
                            val isOnboardingCase = result.data
                            if (isOnboardingCase) {
                                onWalletNotCreated()
                                store.dispatchOnMain(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = false))
                                store.dispatchOnMain(OnboardingSaltPayAction.SetDependencies(manager, config))
                                store.dispatchOnMain(OnboardingSaltPayAction.Update)
                                navigateTo(AppScreen.OnboardingWallet) { onProgressStateChange(it) }
                            } else {
                                delay(DELAY_SDK_DIALOG_CLOSE)
                                onSuccess(scanResponse)
                            }
                        }
                        is Result.Failure -> {
                            delay(DELAY_SDK_DIALOG_CLOSE)
                            SaltPayExceptionHandler.handle(result.error)
                            onFailure(TangemSdkError.ExceptionError(result.error))
                        }
                    }
                }
            } else {
                delay(DELAY_SDK_DIALOG_CLOSE)
                if (scanResponse.card.backupStatus?.isActive == false) {
                    val error = SaltPayActivationError.PutVisaCard
                    SaltPayExceptionHandler.handle(error)
                    onFailure(TangemSdkError.ExceptionError(error))
                } else {
                    onSuccess(scanResponse)
                }
            }
        } else {
            if (OnboardingHelper.isOnboardingCase(scanResponse)) {
                onWalletNotCreated()
                store.dispatchOnMain(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = true))
                val appScreen = OnboardingHelper.whereToNavigate(scanResponse)
                navigateTo(appScreen) { onProgressStateChange(it) }
            } else {
                if (scanResponse.twinsIsTwinned() && !preferencesStorage.wasTwinsOnboardingShown()) {
                    onWalletNotCreated()
                    store.dispatchOnMain(TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly(scanResponse)))
                    navigateTo(AppScreen.OnboardingTwins) { onProgressStateChange(it) }
                } else {
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    onSuccess(scanResponse)
                }
            }
        }
    }

    private suspend inline fun navigateTo(
        screen: AppScreen,
        onProgressStateChange: (showProgress: Boolean) -> Unit,
    ) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        store.dispatchOnMain(NavigationAction.NavigateTo(screen))
        onProgressStateChange(false)
    }
}
