package com.tangem.blockchainsdk.analytics

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.ExceptionHandlerOutput
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import javax.inject.Inject

internal class BlockchainExceptionHandler @Inject constructor(
    private val analyticsErrorHandler: AnalyticsErrorHandler,
) : ExceptionHandlerOutput {
    override fun handleApiSwitch(currentHost: String, nextHost: String, message: String, blockchain: Blockchain) {
        analyticsErrorHandler.sendErrorEvent(
            BlockchainApiExceptionEvent(
                selectedHost = nextHost,
                exceptionHost = currentHost,
                error = message,
                blockchain = blockchain.toNetworkId(),
            ),
        )
    }
}