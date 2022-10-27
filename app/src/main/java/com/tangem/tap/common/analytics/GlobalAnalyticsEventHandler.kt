package com.tangem.tap.common.analytics

import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.SdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler

interface GlobalAnalyticsEventHandler : AnalyticsEventHandler,
    ErrorEventHandler,
    SdkErrorEventHandler,
    ShopifyOrderEventHandler {

    fun addHandler(name: String, handler: AnalyticsEventHandler)

    fun removeHandler(name: String): AnalyticsEventHandler?

    fun attachToAllEvents(key: String, value: String)
}

