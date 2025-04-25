package com.tangem.tap.common.analytics.events

import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.analytics.converters.AnalyticsErrorConverter

class TangemSdkErrorEvent(
    val exception: TangemSdkError,
) : AnalyticsEvent(
    category = "TangemSdk",
    event = "Tangem Sdk Error",
    params = errorConverter.convert(exception),
) {

    companion object {
        private val errorConverter = AnalyticsErrorConverter()
    }
}