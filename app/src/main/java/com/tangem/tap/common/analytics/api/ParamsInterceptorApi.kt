package com.tangem.tap.common.analytics.api

import com.tangem.tap.common.analytics.events.AnalyticsEvent

/**
 * Created by Anton Zhilenkov on 02.11.2022.
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
