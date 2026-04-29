package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.toWrappedList
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.card.ScanFailsCounter
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.mainScope
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LegacyScanProcessor @Inject constructor(
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val trackingContextProxy: TrackingContextProxy,
    private val scanFailsCounter: ScanFailsCounter,
) {

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
    ): CompletionResult<ScanResponse> {
        return tangemSdkManager.scanProduct(
            cardId = cardId,
            allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
            shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
        )
            .doOnFailure { error ->
                onScanFailure(analyticsSource = analyticsSource, error = error, onFailure = {}, onCancel = {})
            }
    }

    @Suppress("LongParameterList")
    suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        onCancel: suspend () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) = withMainContext {
        onProgressStateChange(true)

        tangemSdkManager.changeDisplayedCardIdNumbersCount(null)

        val result = tangemSdkManager.scanProduct(
            cardId = cardId,
            shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
        )

        val analyticsEvent = Basic.CardWasScanned(analyticsSource)

        result
            .doOnFailure { error ->
                scanFailsCounter.onScanFailure(
                    isUserCancelled = error is TangemSdkError.UserCancelled,
                    source = analyticsSource,
                )
                onScanFailure(
                    analyticsSource = analyticsSource,
                    error = error,
                    onFailure = onFailure,
                    onCancel = {
                        mainScope.launch {
                            onProgressStateChange.invoke(false)
                            onCancel()
                        }
                    },
                )
            }
            .doOnSuccess { scanResponse ->
                scanFailsCounter.reset()
                tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

                sendAnalytics(analyticsEvent, scanResponse)

                onScanSuccess(
                    scanResponse = scanResponse,
                    onProgressStateChange = onProgressStateChange,
                    onSuccess = onSuccess,
                    onWalletNotCreated = onWalletNotCreated,
                )
            }
    }

    private fun sendAnalytics(analyticsEvent: AnalyticsEvent, scanResponse: ScanResponse) {
        // this workaround needed to send CardWasScannedEvent without adding a context
        val interceptor = CardContextInterceptor(scanResponse)
        val params = analyticsEvent.params.toMutableMap()
        interceptor.intercept(params)
        analyticsEvent.params = params.toMap()

        Analytics.send(analyticsEvent)
    }

    private suspend inline fun onScanFailure(
        analyticsSource: AnalyticsParam.ScreensSources,
        error: TangemError,
        crossinline onFailure: suspend (TangemError) -> Unit,
        crossinline onCancel: () -> Unit,
    ) {
        if (error is TangemSdkError.CardVerificationFailed) {
            analyticsEventHandler.send(
                event = OnboardingAnalyticsEvent.Error.OfflineAttestationFailed(
                    analyticsSource,
                ),
            )

            val resource = error.localizedDescriptionRes()
            val resId = resource.resId ?: R.string.common_unknown_error
            val resArgs = resource.args.map { it.value }

            uiMessageSender.send(
                message = Dialogs.cardVerificationFailed(
                    errorDescription = resourceReference(id = resId, resArgs.toWrappedList()),
                    onRequestSupport = {
                        mainScope.launch {
                            onCancel()

                            store.inject(DaggerGraphState::sendFeedbackEmailUseCase).invoke(
                                type = FeedbackEmailType.CardAttestationFailed,
                            )
                        }
                    },
                    onCancelClick = { onCancel() },
                ),
            )
        } else {
            onFailure(error)
        }
    }

    @Suppress("LongMethod", "LongParameterList", "MagicNumber")
    private suspend inline fun onScanSuccess(
        scanResponse: ScanResponse,
        crossinline onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        crossinline onWalletNotCreated: suspend () -> Unit,
        crossinline onSuccess: suspend (ScanResponse) -> Unit,
    ) {
        if (OnboardingHelper.isOnboardingCase(scanResponse)) {
            trackingContextProxy.addContext(scanResponse)
            onWalletNotCreated()
            navigateTo(
                AppRoute.Onboarding(
                    scanResponse = scanResponse,
                    mode = AppRoute.Onboarding.Mode.Onboarding,
                ),
            ) { onProgressStateChange(it) }
        } else {
            trackingContextProxy.setContext(scanResponse)

            val wasTwinsOnboardingShown =
                store.inject(DaggerGraphState::wasTwinsOnboardingShownUseCase).invokeSync()

            if (scanResponse.twinsIsTwinned() && !wasTwinsOnboardingShown) {
                onWalletNotCreated()
                navigateTo(
                    AppRoute.Onboarding(
                        scanResponse = scanResponse,
                        mode = AppRoute.Onboarding.Mode.WelcomeOnlyTwin,
                    ),
                ) { onProgressStateChange(it) }
            } else {
                delay(DELAY_SDK_DIALOG_CLOSE)
                onSuccess(scanResponse)
            }
        }
    }

    private suspend inline fun navigateTo(route: AppRoute, onProgressStateChange: (showProgress: Boolean) -> Unit) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        store.dispatchNavigationAction { push(route) }
        onProgressStateChange(false)
    }
}