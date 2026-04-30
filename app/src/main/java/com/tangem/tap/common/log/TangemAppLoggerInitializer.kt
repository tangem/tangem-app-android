package com.tangem.tap.common.log

import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.BuildConfig

/**
 * Tangem app logger
 *
 * @property appLogsStore app logs store
 *
[REDACTED_AUTHOR]
 */
class TangemAppLoggerInitializer(
    private val appLogsStore: AppLogsStore,
) {

    fun initialize() {
        TangemLogger.setLogWriters(
            buildList {
                if (BuildConfig.LOG_ENABLED) {
                    add(LogcatLogWriter())
                }
                add(FileLogWriter(appLogsStore))
            },
        )
    }
}