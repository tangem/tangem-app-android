package com.tangem.core.analytics.utils

import com.tangem.domain.models.scan.ScanResponse

/**
[REDACTED_AUTHOR]
 */
interface AnalyticsContextProxy {

    fun setContext(scanResponse: ScanResponse)

    fun eraseContext()

    fun addContext(scanResponse: ScanResponse)

    fun removeContext()
}