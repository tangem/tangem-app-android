package com.tangem.tap.common.analytics

import com.tangem.core.analytics.Analytics
import com.tangem.domain.analytics.ChangeCardAnalyticsContextUseCase
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.setContext

internal class DefaultChangeCardAnalyticsContextUseCase : ChangeCardAnalyticsContextUseCase {

    override fun invoke(scanResponse: ScanResponse) {
        Analytics.setContext(scanResponse)
    }
}