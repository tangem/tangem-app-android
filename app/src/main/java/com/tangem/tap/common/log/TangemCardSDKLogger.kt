package com.tangem.tap.common.log

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.datasource.local.logs.AppLogsStore

/**
 * CardSDK logger implementation
 *
 * @property levels           logging levels
 * @property messageFormatter message formatter
 * @property appLogsStore     app logs store
 *
[REDACTED_AUTHOR]
 */
@Suppress("UnusedPrivateMember")
internal class TangemCardSDKLogger(
    private val levels: List<Log.Level>,
    private val messageFormatter: LogFormat,
    private val appLogsStore: AppLogsStore,
) : TangemSdkLogger {

    override fun log(message: () -> String, level: Log.Level) {
        // Disabled for now in [REDACTED_JIRA]
        // if (!levels.contains(level)) return
        //
        // appLogsStore.saveLogMessage(message = messageFormatter.format(message, level))
    }
}