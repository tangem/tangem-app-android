package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

class BlockchainExceptionEvent(
    selectedHost: String,
    exceptionHost: String,
    error: String,
) : AnalyticsEvent(
    category = "BlockchainSdk",
    event = "Exception",
    params = mapOf(
        AnalyticsParam.BLOCKCHAIN_SELECTED_HOST to selectedHost,
        AnalyticsParam.BLOCKCHAIN_EXCEPTION_HOST to exceptionHost,
        AnalyticsParam.ERROR_DESCRIPTION to error,
    ),
)
