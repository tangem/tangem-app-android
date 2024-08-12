package com.tangem.tap.data

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * BlockchainSDK logger implementation
 *
 * @property settingsRepository settings repository
 *
 * @author Andrew Khokhlov on 28/02/2024
 */
internal class TangemBlockchainSDKLogger(
    private val settingsRepository: SettingsRepository,
) : BlockchainSDKLogger {

    override fun log(level: BlockchainSDKLogger.Level, message: String) {
        settingsRepository.saveLogMessage(message)
    }
}
