package com.tangem.tap.domain.scanCard

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.models.scan.ScanResponse

// TODO: Remove this object after feature toggle was removed and use ScanCardUseCase instead
internal class DefaultScanCardProcessor(
    private val legacyScanProcessor: LegacyScanProcessor,
    private val useCaseScanProcessor: UseCaseScanProcessor,
    private val cardScanningFeatureToggles: CardScanningFeatureToggles,
) : ScanCardProcessor {
    private val isNewCardScanningEnabled: Boolean
        get() = cardScanningFeatureToggles.isNewCardScanningEnabled

    override suspend fun scan(
        cardId: String?,
        allowsRequestAccessCodeFromRepository: Boolean,
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
    ): CompletionResult<ScanResponse> {
        return if (isNewCardScanningEnabled) {
            useCaseScanProcessor.scan(cardId, allowsRequestAccessCodeFromRepository)
        } else {
            legacyScanProcessor.scan(
                analyticsSource = analyticsSource,
                cardId = cardId,
                allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
                shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
            )
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
        if (isNewCardScanningEnabled) {
            useCaseScanProcessor.scan(
                analyticsSource = analyticsSource,
                cardId = cardId,
                onProgressStateChange = onProgressStateChange,
                onWalletNotCreated = onWalletNotCreated,
                onFailure = onFailure,
                onSuccess = onSuccess,
            )
        } else {
            legacyScanProcessor.scan(
                analyticsSource = analyticsSource,
                shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
                cardId = cardId,
                onProgressStateChange = onProgressStateChange,
                onWalletNotCreated = onWalletNotCreated,
                onCancel = onCancel,
                onFailure = onFailure,
                onSuccess = onSuccess,
            )
        }
    }
}