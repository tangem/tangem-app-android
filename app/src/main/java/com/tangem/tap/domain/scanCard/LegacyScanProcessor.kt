package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.*
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerCallback
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object LegacyScanProcessor {

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        return tangemSdkManager.scanProduct(
            userTokensRepository,
            cardId,
            allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
        )
    }

    @Suppress("LongParameterList")
    suspend fun scan(
        analyticsEvent: AnalyticsEvent?,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onScanStateChange: suspend (scanInProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        disclaimerWillShow: () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) = withMainContext {
        onProgressStateChange(true)
        onScanStateChange(true)

        tangemSdkManager.changeDisplayedCardIdNumbersCount(null)

        val result = tangemSdkManager.scanProduct(
            userTokensRepository = userTokensRepository,
            cardId = cardId,
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

    private suspend inline fun showDisclaimerIfNeed(
        scanResponse: ScanResponse,
        crossinline disclaimerWillShow: () -> Unit = {},
        crossinline nextHandler: suspend (ScanResponse) -> Unit,
        crossinline onFailure: suspend (error: TangemError) -> Unit,
    ) {
        val disclaimer = scanResponse.card.createDisclaimer()
        store.dispatchOnMain(DisclaimerAction.SetDisclaimer(disclaimer))

        if (disclaimer.isAccepted()) {
            nextHandler(scanResponse)
        } else {
            scope.launch {
                delay(DELAY_SDK_DIALOG_CLOSE)
                disclaimerWillShow()
                store.dispatchWithMain(
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
    ) {
        val globalState = store.state.globalState
        val tapWalletManager = globalState.tapWalletManager
        tapWalletManager.updateConfigManager(scanResponse)

        store.dispatchOnMain(TwinCardsAction.IfTwinsPrepareState(scanResponse))

        if (OnboardingHelper.isOnboardingCase(scanResponse)) {
            Analytics.addContext(scanResponse)
            onWalletNotCreated()
            store.dispatchOnMain(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = true))
            val appScreen = OnboardingHelper.whereToNavigate(scanResponse)
            navigateTo(appScreen) { onProgressStateChange(it) }
        } else {
            Analytics.setContext(scanResponse)
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

    private suspend inline fun navigateTo(screen: AppScreen, onProgressStateChange: (showProgress: Boolean) -> Unit) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        store.dispatchOnMain(NavigationAction.NavigateTo(screen))
        onProgressStateChange(false)
    }
}
