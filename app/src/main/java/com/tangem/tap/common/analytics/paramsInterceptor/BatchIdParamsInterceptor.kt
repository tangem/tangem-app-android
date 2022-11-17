package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.tap.common.analytics.api.ParamsInterceptor
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam

/**
 * Created by Anton Zhilenkov on 08.11.2022.
 */
class BatchIdParamsInterceptor(
    val batchId: String,
) : ParamsInterceptor {

    override fun id(): String = this::class.java.simpleName

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = true

    override fun intercept(params: MutableMap<String, String>) {
        params[AnalyticsParam.BatchId] = batchId
    }
}
