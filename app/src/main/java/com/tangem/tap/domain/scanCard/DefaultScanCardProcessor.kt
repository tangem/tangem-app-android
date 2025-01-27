package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store

// TODO: Remove this object after feature toggle was removed and use ScanCardUseCase instead
internal class DefaultScanCardProcessor(
    private val legacyScanProcessor: LegacyScanProcessor,
) : ScanCardProcessor {
    private val isNewCardScanningEnabled: Boolean
        get() = store.inject(DaggerGraphState::cardScanningFeatureToggles).isNewCardScanningEnabled

    override suspend fun scan(
        cardId: String?,
        allowsRequestAccessCodeFromRepository: Boolean,
        analyticsSource: AnalyticsParam.ScreensSources,
    ): CompletionResult<ScanResponse> {
        return if (isNewCardScanningEnabled) {
            UseCaseScanProcessor.scan(cardId, allowsRequestAccessCodeFromRepository)
        } else {
            legacyScanProcessor.scan(
                analyticsSource = analyticsSource,
                cardId = cardId,
                allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
            )
        }
    }

    @Suppress("LongParameterList")
    override suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        cardId: String?,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit,
        onWalletNotCreated: suspend () -> Unit,
        disclaimerWillShow: () -> Unit,
        onCancel: suspend () -> Unit,
        onFailure: suspend (error: TangemError) -> Unit,
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit,
    ) {
        if (isNewCardScanningEnabled) {
            UseCaseScanProcessor.scan(
                analyticsSource = analyticsSource,
                cardId = cardId,
                onProgressStateChange = onProgressStateChange,
                onWalletNotCreated = onWalletNotCreated,
                disclaimerWillShow = disclaimerWillShow,
                onFailure = onFailure,
                onSuccess = onSuccess,
            )
        } else {
            legacyScanProcessor.scan(
                analyticsSource = analyticsSource,
                cardId = cardId,
                onProgressStateChange = onProgressStateChange,
                onWalletNotCreated = onWalletNotCreated,
                disclaimerWillShow = disclaimerWillShow,
                onCancel = onCancel,
                onFailure = onFailure,
                onSuccess = onSuccess,
            )
        }
    }
}