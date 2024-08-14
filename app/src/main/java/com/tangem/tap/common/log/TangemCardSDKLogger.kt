package com.tangem.tap.common.log

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * CardSDK logger implementation
 *
 * @property levels             logging levels
 * @property messageFormatter   message formatter
 * @property settingsRepository settings repository
 *
* [REDACTED_AUTHOR]
 */
internal class TangemCardSDKLogger(
    private val levels: List<Log.Level>,
    private val messageFormatter: LogFormat,
    private val settingsRepository: SettingsRepository,
) : TangemSdkLogger {

    override fun log(message: () -> String, level: Log.Level) {
        if (!levels.contains(level)) return

        settingsRepository.saveLogMessage(message = messageFormatter.format(message, level))
    }
}
