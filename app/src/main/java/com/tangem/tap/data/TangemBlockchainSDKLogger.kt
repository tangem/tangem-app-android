package com.tangem.tap.data

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * BlockchainSDK logger implementation
 *
 * @property settingsRepository settings repository
 * @property dispatchers        coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class TangemBlockchainSDKLogger(
    private val settingsRepository: SettingsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : BlockchainSDKLogger {

    private val scope = CoroutineScope(dispatchers.main)
    private val mutex = Mutex()

    override fun log(level: BlockchainSDKLogger.Level, message: String) {
        scope.launch(dispatchers.main) {
            mutex.withLock {
                settingsRepository.updateAppLogs(message)
            }
        }
    }
}