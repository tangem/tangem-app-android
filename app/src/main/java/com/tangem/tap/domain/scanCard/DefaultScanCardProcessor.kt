package com.tangem.tap.domain.scanCard

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.toWrappedList
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.ScanFailsCounter
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.card.models.ScanConfig
import com.tangem.domain.card.models.ScanError
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
@Suppress("LongParameterList")
internal class DefaultScanCardProcessor @Inject constructor(
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val trackingContextProxy: TrackingContextProxy,
    private val scanFailsCounter: ScanFailsCounter,
    private val appRouter: AppRouter,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase,
    private val onboardingHelper: OnboardingHelper,
    private val tangemSdkManager: TangemSdkManager,
    private val appCoroutineScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : ScanCardProcessor {

    override suspend fun scan(
        cardId: String?,
        allowsRequestAccessCodeFromRepository: Boolean,
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
    ): CompletionResult<ScanResponse> {
        return tangemSdkManager.scanProduct(
            cardId = cardId,
            allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
            shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
            source = analyticsSource,
        )
            .doOnFailure { error ->
                onScanFailure(analyticsSource = analyticsSource, error = error, onFailure = {}, onCancel = {})
            }
    }

    @Suppress("LongParameterList")
    override suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        onCancel: suspend () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) {
        withContext(dispatchers.main) {
            onProgressStateChange(true)

            tangemSdkManager.changeDisplayedCardIdNumbersCount(null)

            val result = tangemSdkManager.scanProduct(
                cardId = cardId,
                shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
                source = analyticsSource,
            )

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
                            appCoroutineScope.launch(dispatchers.main) {
                                onProgressStateChange.invoke(false)
                                onCancel()
                            }
                        },
                    )
                }
                .doOnSuccess { scanResponse ->
                    scanFailsCounter.reset()
                    tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

                    onScanSuccess(
                        scanResponse = scanResponse,
                        onProgressStateChange = onProgressStateChange,
                        onSuccess = onSuccess,
                        onWalletNotCreated = onWalletNotCreated,
                    )
                }
        }
    }

    override suspend fun scan(config: ScanConfig): Either<ScanError, ScanResponse> = withContext(dispatchers.main) {
        if (config.shouldUpdateCardIdDisplayFormat) {
            tangemSdkManager.changeDisplayedCardIdNumbersCount(null)
        }

        val result = tangemSdkManager.scanProduct(
            allowsRequestAccessCodeFromRepository = config.canRequestAccessCodeFromRepository,
            shouldCheckIsAlreadyActivated = config.shouldConfirmWalletOwnership,
            source = config.analyticsSource,
        )

        when (result) {
            is CompletionResult.Failure -> handleScanFailure(config = config, error = result.error).left()
            is CompletionResult.Success -> handleScanSuccess(config = config, scanResponse = result.data)
        }
    }

    private suspend fun handleScanFailure(config: ScanConfig, error: TangemError): ScanError {
        if (config.shouldTrackScanFails) {
            scanFailsCounter.onScanFailure(
                isUserCancelled = error is TangemSdkError.UserCancelled,
                source = config.analyticsSource,
            )
        }

        if (error !is TangemSdkError.CardVerificationFailed) return ScanError.Failure(error)

        suspendCancellableCoroutine { continuation ->
            showCardVerificationFailedDialog(analyticsSource = config.analyticsSource, error = error) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }

        return ScanError.CardVerificationFailed
    }

    private suspend fun handleScanSuccess(
        config: ScanConfig,
        scanResponse: ScanResponse,
    ): Either<ScanError, ScanResponse> {
        if (config.shouldTrackScanFails) scanFailsCounter.reset()
        if (config.shouldUpdateCardIdDisplayFormat) tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

        if (!config.shouldLaunchOnboarding) return scanResponse.right()

        val onboardingRoute = resolveOnboardingRoute(scanResponse)
        delay(DELAY_SDK_DIALOG_CLOSE)

        return if (onboardingRoute != null) {
            appRouter.push(onboardingRoute)
            ScanError.WalletNotCreated.left()
        } else {
            scanResponse.right()
        }
    }

    private suspend fun onScanFailure(
        analyticsSource: AnalyticsParam.ScreensSources,
        error: TangemError,
        onFailure: suspend (TangemError) -> Unit,
        onCancel: () -> Unit,
    ) {
        if (error is TangemSdkError.CardVerificationFailed) {
            showCardVerificationFailedDialog(analyticsSource = analyticsSource, error = error, onClosed = onCancel)
        } else {
            onFailure(error)
        }
    }

    private fun showCardVerificationFailedDialog(
        analyticsSource: AnalyticsParam.ScreensSources,
        error: TangemSdkError.CardVerificationFailed,
        onClosed: () -> Unit,
    ) {
        analyticsEventHandler.send(event = OnboardingAnalyticsEvent.Error.OfflineAttestationFailed(analyticsSource))

        val resource = error.localizedDescriptionRes()
        val resId = resource.resId ?: R.string.common_unknown_error
        val resArgs = resource.args.map { it.value }

        uiMessageSender.send(
            message = Dialogs.cardVerificationFailed(
                errorDescription = resourceReference(id = resId, resArgs.toWrappedList()),
                onRequestSupport = {
                    appCoroutineScope.launch(dispatchers.main) {
                        onClosed()

                        sendFeedbackEmailUseCase.invoke(
                            type = FeedbackEmailType.CardAttestationFailed,
                        )
                    }
                },
                onCancelClick = { onClosed() },
            ),
        )
    }

    private suspend fun onScanSuccess(
        scanResponse: ScanResponse,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        onSuccess: suspend (ScanResponse) -> Unit,
    ) {
        val onboardingRoute = resolveOnboardingRoute(scanResponse)

        if (onboardingRoute != null) {
            onWalletNotCreated()
            navigateTo(route = onboardingRoute, onProgressStateChange = onProgressStateChange)
        } else {
            delay(DELAY_SDK_DIALOG_CLOSE)
            onSuccess(scanResponse)
        }
    }

    /** Returns the onboarding route if the scanned card needs it, and sets the analytics context of the card */
    private suspend fun resolveOnboardingRoute(scanResponse: ScanResponse): AppRoute.Onboarding? {
        if (onboardingHelper.isOnboardingCase(scanResponse)) {
            trackingContextProxy.addContext(scanResponse)
            return AppRoute.Onboarding(scanResponse = scanResponse, mode = AppRoute.Onboarding.Mode.Onboarding)
        }

        trackingContextProxy.setContext(scanResponse)

        val wasTwinsOnboardingShown = wasTwinsOnboardingShownUseCase.invokeSync()

        return if (scanResponse.twinsIsTwinned() && !wasTwinsOnboardingShown) {
            AppRoute.Onboarding(scanResponse = scanResponse, mode = AppRoute.Onboarding.Mode.WelcomeOnlyTwin)
        } else {
            null
        }
    }

    private suspend fun navigateTo(route: AppRoute, onProgressStateChange: suspend (showProgress: Boolean) -> Unit) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        appRouter.push(route)
        onProgressStateChange(false)
    }
}