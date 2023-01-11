package com.tangem.core.analytics.api

import com.tangem.core.analytics.AnalyticsEvent

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
    fun removeParamsInterceptor(interceptor: ParamsInterceptor): ParamsInterceptor?
}
