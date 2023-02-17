package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.tap.common.analytics.events.AnalyticsParam

/**
* [REDACTED_AUTHOR]
 */
class BatchIdParamsInterceptor(
    val batchId: String,
) : ParamsInterceptor {

    override fun id(): String = this::class.java.simpleName

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = true

    override fun intercept(params: MutableMap<String, String>) {
        params[AnalyticsParam.Batch] = batchId
    }
}
