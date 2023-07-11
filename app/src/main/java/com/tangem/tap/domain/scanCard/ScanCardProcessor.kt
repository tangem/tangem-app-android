package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store

// TODO: Remove this object after feature toggle was removed and use ScanCardUseCase instead
internal object ScanCardProcessor {
    private val isNewCardScanningEnabled: Boolean
        get() = store.state.daggerGraphState
            .get(DaggerGraphState::customTokenFeatureToggles)
            .isNewCardScanningEnabled

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        return if (isNewCardScanningEnabled) {
            UseCaseScanProcessor.scan(cardId, allowsRequestAccessCodeFromRepository)
        } else {
            LegacyScanProcessor.scan(cardId, allowsRequestAccessCodeFromRepository)
        }
    }

    @Suppress("LongParameterList")
    suspend fun scan(
        analyticsEvent: AnalyticsEvent? = null,
        cardId: String? = null,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit = {},
        onScanStateChange: suspend (scanInProgress: Boolean) -> Unit = {},
        onWalletNotCreated: suspend () -> Unit = {},
        disclaimerWillShow: () -> Unit = {},
        onFailure: suspend (error: TangemError) -> Unit = {},
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit = {},
    ) {
        if (isNewCardScanningEnabled) {
            UseCaseScanProcessor.scan(
                analyticsEvent,
                cardId,
                onProgressStateChange,
                onScanStateChange,
                onWalletNotCreated,
                disclaimerWillShow,
                onFailure,
                onSuccess,
            )
        } else {
            LegacyScanProcessor.scan(
                analyticsEvent,
                cardId,
                onProgressStateChange,
                onScanStateChange,
                onWalletNotCreated,
                disclaimerWillShow,
                onFailure,
                onSuccess,
            )
        }
    }
}