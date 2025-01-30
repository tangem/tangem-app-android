package com.tangem.tap.common.analytics

import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler

/**
[REDACTED_AUTHOR]
 */
class AnalyticsFactory {

    private val builders = mutableListOf<AnalyticsHandlerBuilder>()
    private val filters = mutableListOf<AnalyticsEventFilter>()
    private val interceptors = mutableListOf<ParamsInterceptor>()

    fun setupHandlers(isGoogleServicesAvailable: Boolean, isHuaweiServicesAvailable: Boolean) {
        builders.add(AmplitudeAnalyticsHandler.Builder())

        if (isGoogleServicesAvailable) {
            builders.add(FirebaseAnalyticsHandler.Builder())
        } else if (isHuaweiServicesAvailable) {
            // todo huawei [REDACTED_TASK_KEY]
            return // builders.add(AppGalleryAnalyticsHandler.Builder())
        }
    }

    fun addFilter(filter: AnalyticsEventFilter) {
        filters.add(filter)
    }

    fun build(analytics: Analytics, data: AnalyticsHandlerBuilder.Data) {
        builders.mapNotNull { it.build(data) }.forEach { analytics.addHandler(it.id(), it) }
        filters.forEach { analytics.addFilter(it) }
        interceptors.forEach { analytics.addParamsInterceptor(it) }

        builders.clear()
        filters.clear()
        interceptors.clear()
    }
}