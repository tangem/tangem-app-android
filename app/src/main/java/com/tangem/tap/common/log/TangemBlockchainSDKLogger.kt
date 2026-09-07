package com.tangem.tap.common.log

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.utils.logging.TangemLogger

/**
 * BlockchainSDK logger implementation
 *
[REDACTED_AUTHOR]
 */
internal class TangemBlockchainSDKLogger : BlockchainSDKLogger {

    override fun log(level: BlockchainSDKLogger.Level, message: String) {
        TangemLogger.withTag(tag = "BlockchainSDK_${level.name}").i(message)
    }
}