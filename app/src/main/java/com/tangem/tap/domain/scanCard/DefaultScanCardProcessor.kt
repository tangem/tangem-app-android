package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
// [REDACTED_TODO_COMMENT]
internal class DefaultScanCardProcessor : ScanCardProcessor {
    private val isNewCardScanningEnabled: Boolean
        get() = store.inject(DaggerGraphState::customTokenFeatureToggles).isNewCardScanningEnabled

    override suspend fun scan(
        cardId: String?,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<ScanResponse> {
        return if (isNewCardScanningEnabled) {
            UseCaseScanProcessor.scan(cardId, allowsRequestAccessCodeFromRepository)
        } else {
            LegacyScanProcessor.scan(cardId, allowsRequestAccessCodeFromRepository)
        }
    }

    @Suppress("LongParameterList")
    override suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onScanStateChange: suspend (scanInProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        disclaimerWillShow: () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) {
        if (isNewCardScanningEnabled) {
            UseCaseScanProcessor.scan(
                analyticsSource,
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
                analyticsSource,
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
