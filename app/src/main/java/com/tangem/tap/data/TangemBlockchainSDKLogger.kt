package com.tangem.tap.data

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.datasource.local.logs.AppLogsStore
import timber.log.Timber

/**
 * BlockchainSDK logger implementation
 *
 * @property appLogsStore app logs store
 *
[REDACTED_AUTHOR]
 */
internal class TangemBlockchainSDKLogger(
    private val appLogsStore: AppLogsStore,
) : BlockchainSDKLogger {

    override fun log(level: BlockchainSDKLogger.Level, message: String) {
        Timber.d(message)
        appLogsStore.saveLogMessage(tag = "BlockchainSDK_${level.name}", message)
    }
}