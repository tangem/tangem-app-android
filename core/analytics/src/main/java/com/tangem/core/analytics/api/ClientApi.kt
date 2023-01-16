package com.tangem.core.analytics.api

/**
 * Created by Anton Zhilenkov on 23/09/2022.
 */
interface EventLogger {
    fun logEvent(
        event: String,
        params: Map<String, String> = emptyMap(),
    )
}

interface ErrorEventLogger {
    fun logErrorEvent(
        error: Throwable,
        params: Map<String, String> = emptyMap(),
    )
}
