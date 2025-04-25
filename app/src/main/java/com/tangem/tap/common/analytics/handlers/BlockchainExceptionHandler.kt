package com.tangem.tap.common.analytics.handlers

import com.tangem.blockchain.common.ExceptionHandlerOutput
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.tap.common.analytics.events.BlockchainApiExceptionEvent
import javax.inject.Inject

class BlockchainExceptionHandler @Inject constructor(
    private val analyticsErrorHandler: AnalyticsErrorHandler,
) : ExceptionHandlerOutput {
    override fun handleApiSwitch(currentHost: String, nextHost: String, message: String) {
        analyticsErrorHandler.sendErrorEvent(
            BlockchainApiExceptionEvent(
                selectedHost = nextHost,
                exceptionHost = currentHost,
                error = message,
            ),
        )
    }
}