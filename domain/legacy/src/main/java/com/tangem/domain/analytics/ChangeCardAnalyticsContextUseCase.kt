package com.tangem.domain.analytics

import com.tangem.domain.models.scan.ScanResponse

interface ChangeCardAnalyticsContextUseCase {

    operator fun invoke(scanResponse: ScanResponse)
}