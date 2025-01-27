package com.tangem.domain.card

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.scan.ScanResponse

interface ScanCardProcessor {

    suspend fun scan(
        cardId: String? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
        analyticsSource: AnalyticsParam.ScreensSources,
    ): CompletionResult<ScanResponse>

    suspend fun scan(
        analyticsSource: AnalyticsParam.ScreensSources,
        cardId: String? = null,
        onProgressStateChange: suspend (showProgress: Boolean) -> Unit = {},
        onWalletNotCreated: suspend () -> Unit = {},
        disclaimerWillShow: () -> Unit = {},
        onCancel: suspend () -> Unit = {},
        onFailure: suspend (error: TangemError) -> Unit = {},
        onSuccess: suspend (scanResponse: ScanResponse) -> Unit = {},
    )
}