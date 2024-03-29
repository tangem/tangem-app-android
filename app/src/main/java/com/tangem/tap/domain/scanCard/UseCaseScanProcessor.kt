package com.tangem.tap.domain.scanCard

import arrow.fx.coroutines.resourceScope
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.domain.scanCard.chains.*
import com.tangem.tap.domain.scanCard.utils.ScanCardExceptionConverter
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay

internal object UseCaseScanProcessor {
    private val scanCardExceptionConverter = ScanCardExceptionConverter()

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        val scanCardUseCase = store.inject(DaggerGraphState::scanCardUseCase)
        return scanCardUseCase(cardId, allowsRequestAccessCodeFromRepository)
            .fold(
                ifLeft = { CompletionResult.Failure(scanCardExceptionConverter.convertBack(it)) },
                ifRight = { CompletionResult.Success(it) },
            )
    }

    @Suppress("LongParameterList")
    suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onScanStateChange: suspend (scanInProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        disclaimerWillShow: () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) = progressScope(onProgressStateChange) {
        onScanStateChange(true)

        val scanCardUseCase = store.inject(DaggerGraphState::scanCardUseCase)
        val chains = buildList {
            add(ScanningFinishedChain { onScanStateChange(false) })
            add(AnalyticsChain(Basic.CardWasScanned(analyticsSource)))
            add(DisclaimerChain(store, disclaimerWillShow))
            add(CheckForOnboardingChain(store, store.state.globalState.tapWalletManager))
        }

        scanCardUseCase(cardId, afterScanChains = chains)
            .map { onSuccess(it) }
            .mapLeft { proceedWithException(it, onWalletNotCreated, onFailure) }
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
            -> onFailure(scanCardExceptionConverter.convertBack(exception))
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
            is ScanChainException.DisclaimerWasCanceled,
            -> onFailure(scanCardExceptionConverter.convertBack(exception))
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

    private suspend inline fun navigateTo(screen: AppScreen) {
        delay(DELAY_SDK_DIALOG_CLOSE)
        store.dispatchOnMain(NavigationAction.NavigateTo(screen))
    }
}