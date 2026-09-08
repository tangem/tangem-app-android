package com.tangem.tap.common.log

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.utils.logging.TangemLogger

/**
 * BlockchainSDK logger implementation
 *
[REDACTED_AUTHOR]
 */
internal class TangemBlockchainSDKLogger : BlockchainSDKLogger {

    override fun log(level: BlockchainSDKLogger.Level, tag: String, message: String, throwable: Throwable?) {
        val logger = TangemLogger.withTag(tag = "BlockchainSDK_$tag")

        when (level) {
            BlockchainSDKLogger.Level.DEBUG -> logger.d(message, throwable)
            BlockchainSDKLogger.Level.INFO -> logger.i(message, throwable)
            BlockchainSDKLogger.Level.ERROR -> logger.e(message, throwable)
        }
    }
}