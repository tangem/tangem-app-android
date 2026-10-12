package com.tangem.domain.card

import arrow.core.Either
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.card.models.ScanConfig
import com.tangem.domain.card.models.ScanError
import com.tangem.domain.models.scan.ScanResponse

interface ScanCardProcessor {

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
    ): CompletionResult<ScanResponse>

    suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        shouldCheckIsAlreadyActivated: Boolean,
        cardId: String? = null,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit = {},
        onWalletNotCreated: suspend () -> Unit = {},
        onCancel: suspend () -> Unit = {},
        onFailure: suspend (error: TangemError) -> Unit = {},
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit = {},
    )

    /**
     * Scans a card with the given [config]. The caller owns the progress indicator; a card attestation failure
     * returns only after the user has closed the warning dialog.
     */
    suspend fun scan(config: ScanConfig): Either<ScanError, ScanResponse>
}