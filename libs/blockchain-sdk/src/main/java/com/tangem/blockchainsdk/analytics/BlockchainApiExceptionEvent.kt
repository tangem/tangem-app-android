package com.tangem.blockchainsdk.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal class BlockchainApiExceptionEvent(
    selectedHost: String,
    exceptionHost: String,
    error: String,
    blockchain: String,
) : AnalyticsEvent(
    category = "BlockchainSdk",
    event = "Exception",
    params = mapOf(
        AnalyticsParam.BLOCKCHAIN_SELECTED_HOST to selectedHost,
        AnalyticsParam.BLOCKCHAIN_EXCEPTION_HOST to exceptionHost,
        AnalyticsParam.ERROR_DESCRIPTION to error,
        AnalyticsParam.BLOCKCHAIN to blockchain,
    ),
)