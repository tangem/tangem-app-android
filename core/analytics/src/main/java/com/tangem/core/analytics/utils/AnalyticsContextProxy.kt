package com.tangem.core.analytics.utils

import com.tangem.domain.models.scan.ScanResponse

/**
 * @author Andrew Khokhlov on 31/07/2024
 */
interface AnalyticsContextProxy {

    fun setContext(scanResponse: ScanResponse)

    fun eraseContext()

    fun addContext(scanResponse: ScanResponse)

    fun removeContext()
}
