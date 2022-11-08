package com.tangem.tap.common.analytics.api

import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.AnalyticsAnOld
import com.tangem.tap.common.analytics.AnalyticsParamAnOld

/**
[REDACTED_AUTHOR]
 */
interface ErrorEventHandler {
    fun send(
        error: Throwable,
        params: Map<String, String> = emptyMap(),
    )
}

interface SdkErrorEventHandler : CardSdkErrorEventHandler, BlockchainSdkErrorEventHandler

interface CardSdkErrorEventHandler {
    fun send(
        error: TangemSdkError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String> = emptyMap(),
        card: Card? = null,
    )
}

interface BlockchainSdkErrorEventHandler {
    fun send(
        error: BlockchainError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String> = mapOf(),
        card: Card? = null,
    )
}