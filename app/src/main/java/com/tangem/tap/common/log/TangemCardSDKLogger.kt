package com.tangem.tap.common.log

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CardSDK logger implementation
 *
 * @property levels             logging levels
 * @property messageFormatter   message formatter
 * @property settingsRepository settings repository
 * @property dispatchers        coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class TangemCardSDKLogger(
    private val levels: List<Log.Level>,
    private val messageFormatter: LogFormat,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : TangemSdkLogger {

    private val scope = CoroutineScope(dispatchers.main)
    private val mutex = Mutex()

    override fun log(message: () -> String, level: Log.Level) {
        if (!levels.contains(level)) return

        scope.launch(dispatchers.main) {
            mutex.withLock {
                settingsRepository.updateAppLogs(message = messageFormatter.format(message, level))
            }
        }
    }
}