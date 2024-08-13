package com.tangem.tap.data

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * BlockchainSDK logger implementation
 *
 * @property settingsRepository settings repository
 *
* [REDACTED_AUTHOR]
 */
internal class TangemBlockchainSDKLogger(
    private val settingsRepository: SettingsRepository,
) : BlockchainSDKLogger {

    override fun log(level: BlockchainSDKLogger.Level, message: String) {
        settingsRepository.saveLogMessage(message)
    }
}
