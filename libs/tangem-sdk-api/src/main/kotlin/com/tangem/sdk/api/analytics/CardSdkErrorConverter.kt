package com.tangem.sdk.api.analytics

import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.utils.converter.Converter

class CardSdkErrorConverter : Converter<TangemSdkError, Map<String, String>> {

    override fun convert(value: TangemSdkError): Map<String, String> {
        if (value is TangemSdkError.UserCancelled) return emptyMap()

        return mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.CardSdk.value,
            AnalyticsParam.ERROR_CODE to value.code.toString(),
            AnalyticsParam.ERROR_DESCRIPTION to value.toString(),
        )
    }
}