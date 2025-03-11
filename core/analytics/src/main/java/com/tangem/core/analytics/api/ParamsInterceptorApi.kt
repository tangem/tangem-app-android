package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
 */
interface ParamsInterceptor {
    fun id(): String
    fun canBeAppliedTo(event: AnalyticsEvent): Boolean
    fun intercept(params: MutableMap<String, String>)
}

interface ParamsInterceptorHolder {
    fun addParamsInterceptor(interceptor: ParamsInterceptor)
    fun removeParamsInterceptor(interceptorId: String): ParamsInterceptor?
}
