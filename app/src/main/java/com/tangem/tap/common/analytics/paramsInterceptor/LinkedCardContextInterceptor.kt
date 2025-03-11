package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.scan.ScanResponse

/**
* [REDACTED_AUTHOR]
 */
class LinkedCardContextInterceptor(
    scanResponse: ScanResponse,
    val parent: LinkedCardContextInterceptor? = null,
) : ParamsInterceptor {

    private val contextInterceptor = CardContextInterceptor(scanResponse)

    override fun id(): String = LinkedCardContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = contextInterceptor.canBeAppliedTo(event)

    override fun intercept(params: MutableMap<String, String>) = contextInterceptor.intercept(params)

    companion object {
        fun id(): String = LinkedCardContextInterceptor::class.java.simpleName
    }
}
