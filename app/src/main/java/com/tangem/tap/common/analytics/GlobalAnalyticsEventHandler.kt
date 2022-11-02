package com.tangem.tap.common.analytics

import com.tangem.tap.common.analytics.api.AnalyticsEventFilter
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.SdkErrorEventHandler

interface GlobalAnalyticsEventHandler : AnalyticsEventHandler,
    ErrorEventHandler,
    SdkErrorEventHandler {

    fun addHandler(name: String, handler: AnalyticsEventHandler)

    fun removeHandler(name: String): AnalyticsEventHandler?

    fun addFilter(filter: AnalyticsEventFilter)

    fun removeFilter(filter: AnalyticsEventFilter)

    fun attachToAllEvents(key: String, value: String)
}
