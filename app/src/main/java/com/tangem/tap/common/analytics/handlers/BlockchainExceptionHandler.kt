package com.tangem.tap.common.analytics.handlers

import com.tangem.blockchain.common.ExceptionHandlerOutput
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.events.BlockchainExceptionEvent
import javax.inject.Inject

class BlockchainExceptionHandler @Inject constructor(
    private val analyticsHandler: AnalyticsEventHandler,
) : ExceptionHandlerOutput {
    override fun handleApiSwitch(currentHost: String, nextHost: String, message: String) {
        analyticsHandler.send(
            BlockchainExceptionEvent(
                selectedHost = nextHost,
                exceptionHost = currentHost,
                error = message,
            ),
        )
    }
}
