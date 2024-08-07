package com.tangem.tap.common.analytics

import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.utils.AnalyticsContextProxy
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.addContext
import com.tangem.tap.common.extensions.eraseContext
import com.tangem.tap.common.extensions.removeContext
import com.tangem.tap.common.extensions.setContext

/**
[REDACTED_AUTHOR]
 */
internal class DefaultAnalyticsContextProxy : AnalyticsContextProxy {

    override fun setContext(scanResponse: ScanResponse) {
        Analytics.setContext(scanResponse)
    }

    override fun eraseContext() {
        Analytics.eraseContext()
    }

    override fun addContext(scanResponse: ScanResponse) {
        Analytics.addContext(scanResponse)
    }

    override fun removeContext() {
        Analytics.removeContext()
    }
}