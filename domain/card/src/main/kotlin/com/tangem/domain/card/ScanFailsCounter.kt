package com.tangem.domain.card

import com.tangem.core.analytics.models.AnalyticsParam

interface ScanFailsCounter {

    fun reset()

    fun onScanFailure(isUserCancelled: Boolean, source: AnalyticsParam.ScreensSources)
}