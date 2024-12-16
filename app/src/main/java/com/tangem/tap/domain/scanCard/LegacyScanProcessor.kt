package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.domain.common.TapWorkarounds.canSkipBackup
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupStartedSource
import com.tangem.tap.mainScope
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object LegacyScanProcessor {

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        return tangemSdkManager.scanProduct(
            cardId = cardId,
            allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
        )
    }

    @Suppress("LongParameterList")
    suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        disclaimerWillShow: () -> Unit,
        onCancel: suspend () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) = withMainContext {
        onProgressStateChange(true)

        tangemSdkManager.changeDisplayedCardIdNumbersCount(null)

        val result = tangemSdkManager.scanProduct(cardId)

        val analyticsEvent = Basic.CardWasScanned(analyticsSource)
        store.dispatchOnMain(GlobalAction.ScanFailsCounter.ChooseBehavior(result, analyticsSource))

        result
            .doOnFailure { error ->
                onFailure(error)
            }
            .doOnSuccess { scanResponse ->
                tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

                sendAnalytics(analyticsEvent, scanResponse)

                showDisclaimerIfNeed(
                    scanResponse = scanResponse,
                    disclaimerWillShow = disclaimerWillShow,
                    onFailure = onFailure,
                    nextHandler = { scanResponse2 ->
                        onScanSuccess(
                            scanResponse = scanResponse2,
                            onProgressStateChange = onProgressStateChange,
                            onSuccess = onSuccess,
                            onWalletNotCreated = onWalletNotCreated,
                            onCancel = onCancel,
                        )
                    },
                )
            }
    }

    private fun sendAnalytics(analyticsEvent: AnalyticsEvent?, scanResponse: ScanResponse) {
        analyticsEvent?.let {
            // this workaround needed to send CardWasScannedEvent without adding a context
            val interceptor = CardContextInterceptor(scanResponse)
            val params = it.params.toMutableMap()
            interceptor.intercept(params)
            it.params = params.toMap()

            Analytics.send(it)
        }
    }

    // TODO: [REDACTED_JIRA]
    @Suppress("UnusedPrivateMember")
    private suspend inline fun showDisclaimerIfNeed(
        scanResponse: ScanResponse,
        crossinline disclaimerWillShow: () -> Unit = {},
        crossinline nextHandler: suspend (ScanResponse) -> Unit,
        crossinline onFailure: suspend (error: TangemError) -> Unit,
    ) {
        val disclaimer = scanResponse.card.createDisclaimer()

        if (disclaimer.isAccepted()) {
            nextHandler(scanResponse)
        } else {
            scope.launch {
                delay(DELAY_SDK_DIALOG_CLOSE)

                withContext(Dispatchers.Main.immediate) {
                    disclaimerWillShow()

                    store.dispatchNavigationAction {
                        push(AppRoute.Disclaimer(isTosAccepted = false))
                    }
                }
            }
        }
    }

    @Suppress("LongMethod", "MagicNumber")
    private suspend inline fun onScanSuccess(
        scanResponse: ScanResponse,
        crossinline onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        crossinline onWalletNotCreated: suspend () -> Unit,
        crossinline onCancel: suspend () -> Unit,
        crossinline onSuccess: suspend (ScanResponse) -> Unit,
    ) {
        store.dispatchOnMain(TwinCardsAction.IfTwinsPrepareState(scanResponse))

        checkCardWasUsedInApp(
            scanResponse = scanResponse,
            onCancel = {
                mainScope.launch {
                    onProgressStateChange.invoke(false)
                    onCancel()
                }
            },
        ) {
            if (OnboardingHelper.isOnboardingCase(scanResponse)) {
                Analytics.addContext(scanResponse)
                onWalletNotCreated()
                // must check skip backup using card canSkipBackup
                store.dispatchOnMain(
                    GlobalAction.Onboarding.Start(
                        scanResponse = scanResponse,
                        source = BackupStartedSource.Onboarding,
                        canSkipBackup = scanResponse.card.canSkipBackup,
                    ),
                )
                val appScreen = OnboardingHelper.whereToNavigate(scanResponse)
                navigateTo(appScreen) { onProgressStateChange(it) }
            } else {
                Analytics.setContext(scanResponse)

                val wasTwinsOnboardingShown =
                    store.inject(DaggerGraphState::wasTwinsOnboardingShownUseCase).invokeSync()

                if (scanResponse.twinsIsTwinned() && !wasTwinsOnboardingShown) {
                    onWalletNotCreated()
                    store.dispatchOnMain(TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly(scanResponse)))
                    navigateTo(AppRoute.OnboardingTwins) { onProgressStateChange(it) }
                } else {
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    onSuccess(scanResponse)
                }
            }
        }
    }

    /**
     * Checks if card has password and never login at this app
     * Show alert in this case
     */
    private suspend fun checkCardWasUsedInApp(
        scanResponse: ScanResponse,
        onCancel: () -> Unit,
        onSuccess: suspend () -> Unit,
    ) {
        val userWalletId = runCatching { UserWalletIdBuilder.card(scanResponse.card).build() }.getOrNull()
        if (userWalletId == null) {
            onSuccess()
            return
        }
        val appPrefStoreStore = store.inject(DaggerGraphState::appPreferencesStore)
        val tokens = appPrefStoreStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
        )
        if (scanResponse.card.isAccessCodeSet && tokens == null) {
            store.dispatchDialogShow(
                AppDialog.WalletAlreadyWasUsedDialog(
                    onOk = { mainScope.launch { onSuccess() } },
                    onSupportClick = {
                        val cardInfo =
                            store.inject(DaggerGraphState::getCardInfoUseCase).invoke(scanResponse).getOrNull()
                                ?: error("CardInfo must be not null")

                        scope.launch {
                            store.inject(DaggerGraphState::sendFeedbackEmailUseCase)
                                .invoke(type = FeedbackEmailType.PreActivatedWallet(cardInfo))
                        }
                        onCancel()
                    },
                    onCancel = { onCancel() },
                ),
            )
        } else {
            onSuccess()
        }
    }

    private suspend inline fun navigateTo(route: AppRoute, onProgressStateChange: (showProgress: Boolean) -> Unit) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        store.dispatchNavigationAction { push(route) }
        onProgressStateChange(false)
    }
}