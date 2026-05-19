package com.tangem.tap.domain.scanCard

import arrow.fx.coroutines.resourceScope
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.ScanFailsRequester
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.tap.common.analytics.events.TangemSdkErrorEvent
import com.tangem.tap.domain.scanCard.chains.AnalyticsChain
import com.tangem.tap.domain.scanCard.chains.CheckForOnboardingChain
import com.tangem.tap.domain.scanCard.chains.FailedScansCounterChain
import com.tangem.tap.domain.scanCard.chains.ScanChainException
import com.tangem.tap.domain.scanCard.utils.ScanCardExceptionConverter
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.scope
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("LongParameterList")
internal class UseCaseScanProcessor @Inject constructor(
    private val scanCardUseCase: ScanCardUseCase,
    private val scanFailsRequester: ScanFailsRequester,
    private val appRouter: AppRouter,
    private val trackingContextProxy: TrackingContextProxy,
    private val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase,
    private val onboardingHelper: OnboardingHelper,
) {
    private val scanCardExceptionConverter = ScanCardExceptionConverter()

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        return scanCardUseCase(cardId, allowsRequestAccessCodeFromRepository)
            .fold(
                ifLeft = { scanCardException ->
                    val error = scanCardExceptionConverter.convertBack(scanCardException)

                    Analytics.sendErrorEvent(TangemSdkErrorEvent(error))
                    CompletionResult.Failure(error)
                },
                ifRight = { CompletionResult.Success(it) },
            )
    }

    @Suppress("LongParameterList")
    suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) = progressScope(onProgressStateChange) {
        val chains = buildList {
            add(
                FailedScansCounterChain(
                    { showScanFailsDialog(analyticsSource) },
                ),
            )
            add(AnalyticsChain(Basic.CardWasScanned(analyticsSource)))
            add(CheckForOnboardingChain(trackingContextProxy, wasTwinsOnboardingShownUseCase, onboardingHelper))
        }

        scanCardUseCase(cardId, afterScanChains = chains).fold(
            ifLeft = { proceedWithException(it, onWalletNotCreated, onFailure) },
            ifRight = { onSuccess(it) },
        )
    }

    private fun showScanFailsDialog(source: AnalyticsParam.ScreensSources) {
        scope.launch {
            scanFailsRequester.show(source)
        }
    }

    private suspend fun proceedWithException(
        exception: ScanCardException,
        onWalletNotCreated: suspend () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
    ) {
        when (exception) {
            is ScanCardException.ChainException -> proceedWithScanChainException(
                exception,
                onWalletNotCreated,
            )
            is ScanCardException.UnknownException,
            is ScanCardException.UserCancelled,
            is ScanCardException.WrongAccessCode,
            is ScanCardException.WrongCardId,
            -> {
                val error = scanCardExceptionConverter.convertBack(exception)

                Analytics.sendException(
                    ExceptionAnalyticsEvent(
                        exception = error,
                        params = mapOf("Event" to "Scan"),
                    ),
                )
                onFailure(error)
            }
        }
    }

    private suspend fun proceedWithScanChainException(
        exception: ScanCardException.ChainException,
        onWalletNotCreated: suspend () -> Unit,
    ) {
        when (exception) {
            is ScanChainException.OnboardingNeeded -> {
                navigateTo(exception.onboardingRoute)
                onWalletNotCreated()
            }
        }
    }

    private suspend fun progressScope(
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        action: suspend () -> Unit,
    ) = resourceScope {
        install(
            acquire = { onProgressStateChange(true) },
            release = { _, _ -> onProgressStateChange(false) },
        )

        action()
    }

    private suspend inline fun navigateTo(route: AppRoute) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        appRouter.push(route)
    }
}