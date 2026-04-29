package com.tangem.tap.domain.scanCard

import arrow.fx.coroutines.resourceScope
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.analytics.events.TangemSdkErrorEvent
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.domain.scanCard.chains.*
import com.tangem.tap.domain.scanCard.utils.ScanCardExceptionConverter
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object UseCaseScanProcessor {
    private val scanCardExceptionConverter = ScanCardExceptionConverter()

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        val scanCardUseCase = store.inject(DaggerGraphState::scanCardUseCase)

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
        val scanCardUseCase = store.inject(DaggerGraphState::scanCardUseCase)
        val chains = buildList {
            add(
                FailedScansCounterChain(
                    { showScanFailsDialog(analyticsSource) },
                ),
            )
            add(AnalyticsChain(Basic.CardWasScanned(analyticsSource)))
            add(CheckForOnboardingChain(store))
        }

        scanCardUseCase(cardId, afterScanChains = chains).fold(
            ifLeft = { proceedWithException(it, onWalletNotCreated, onFailure) },
            ifRight = { onSuccess(it) },
        )
    }

    private fun showScanFailsDialog(source: AnalyticsParam.ScreensSources) {
        scope.launch {
            store.inject(DaggerGraphState::scanFailsRequester).show(source)
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
                onFailure,
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
        onFailure: suspend (error: TangemError) -> Unit,
    ) {
        when (exception) {
            is ScanChainException.OnboardingNeeded -> {
                navigateTo(exception.onboardingRoute)
                onWalletNotCreated()
            }
            is ScanChainException.DisclaimerWasCanceled -> {
                onFailure(scanCardExceptionConverter.convertBack(exception))
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
        store.dispatchNavigationAction { push(route) }
    }
}