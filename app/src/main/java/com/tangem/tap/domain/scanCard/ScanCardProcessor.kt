package com.tangem.tap.domain.scanCard

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.backup.BackupService
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.backupService
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.paramsInterceptor.BatchIdParamsInterceptor
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.primaryCardIsSaltPayVisa
import com.tangem.tap.common.redux.AppDialog
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
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userTokensRepository
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Create repository for that
object ScanCardProcessor {
    suspend fun scan(
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
                onProgressStateChange(false)
                onScanStateChange(false)
                onFailure(error)
            }
            .doOnSuccess { scanResponse ->
                tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

                onScanStateChange(false)
                checkForUnfinishedBackupForSaltPay(
                    backupService = backupService,
                    scanResponse = scanResponse,
                    onProgressStateChange = { onProgressStateChange(it) },
                    nextHandler = { scanResponse1 ->
                        showDisclaimerIfNeed(
                            scanResponse = scanResponse1,
                            onProgressStateChange = onProgressStateChange,
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

    /**
     * It checks only the SaltPay cards. To check for unfinished backups for the standard Wallet cards
     * see BackupAction.CheckForUnfinishedBackup
     * If user touches card other than Visa SaltPay - show dialog and block next processing
     */
    private inline fun checkForUnfinishedBackupForSaltPay(
        backupService: BackupService,
        scanResponse: ScanResponse,
        onProgressStateChange: (showProgress: Boolean) -> Unit,
        nextHandler: (ScanResponse) -> Unit,
    ) {
        if (!backupService.hasIncompletedBackup || !backupService.primaryCardIsSaltPayVisa()) {
            nextHandler(scanResponse)
            return
        }

        val isTheSamePrimaryCard = backupService.primaryCardId
            ?.let { it == scanResponse.card.cardId }
            ?: false

        if (scanResponse.isSaltPayWallet() || !isTheSamePrimaryCard) {
            onProgressStateChange(false)
            showSaltPayTapVisaLogoCardDialog()
        } else {
            nextHandler(scanResponse)
        }
    }

    private suspend inline fun showDisclaimerIfNeed(
        scanResponse: ScanResponse,
        crossinline disclaimerWillShow: () -> Unit = {},
        crossinline onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
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
                                    onProgressStateChange(false)
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
        Analytics.send(IntroductionProcess.CardWasScanned())

        val globalState = store.state.globalState
        val tapWalletManager = globalState.tapWalletManager
        tapWalletManager.updateConfigManager(scanResponse)

        Analytics.addParamsInterceptor(BatchIdParamsInterceptor(scanResponse.card.batchId))

        store.dispatchOnMain(TwinCardsAction.IfTwinsPrepareState(scanResponse))

        if (scanResponse.isSaltPay()) {
            if (scanResponse.isSaltPayVisa()) {
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
                                onProgressStateChange(false)
                            }
                        }
                        is Result.Failure -> {
                            SaltPayExceptionHandler.handle(result.error)
                            delay(DELAY_SDK_DIALOG_CLOSE)
                            onFailure(TangemSdkError.ExceptionError(result.error))
                            onProgressStateChange(false)
                        }
                    }
                }
            } else {
                delay(DELAY_SDK_DIALOG_CLOSE)
                if (scanResponse.card.backupStatus?.isActive == false) {
                    showSaltPayTapVisaLogoCardDialog()
                } else {
                    onSuccess(scanResponse)
                }
                onProgressStateChange(false)
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
                    onProgressStateChange(false)
                }
            }
        }
    }

    private fun showSaltPayTapVisaLogoCardDialog() {
        store.dispatchDialogShow(
            AppDialog.SimpleOkDialogRes(
                headerId = R.string.saltpay_error_empty_backup_title,
                messageId = R.string.saltpay_error_empty_backup_message,
            ),
        )
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