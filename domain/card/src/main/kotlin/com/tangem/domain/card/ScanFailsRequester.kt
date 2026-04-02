package com.tangem.domain.card

import com.tangem.core.analytics.models.AnalyticsParam

interface ScanFailsRequester {

    suspend fun show(source: AnalyticsParam.ScreensSources): Result

    sealed class Result {
        data object Dismissed : Result()
    }
}