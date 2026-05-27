package com.tangem.tap.common.log

import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger

/**
 * [TangemLogger.LogWriter] that persists log entries to [AppLogsStore].
 *
 * Only [Severity.Error] and [Severity.Info] are written. The `shouldSanitize` flag is forwarded to
 * [AppLogsStore.saveLogMessage], so callers that deliberately log unsanitized content
 * (`shouldSanitize = false`) bypass the sanitizer.
 */
internal class FileLogWriter(
    private val appLogsStore: AppLogsStore,
) : TangemLogger.LogWriter {

    override fun isLoggable(severity: Severity, tag: String): Boolean {
        return severity == Severity.Error || severity == Severity.Info
    }

    override fun write(
        severity: Severity,
        tag: String,
        message: String,
        throwable: Throwable?,
        shouldSanitize: Boolean,
    ) {
        appLogsStore.saveLogMessage(
            tag = tag,
            message = message,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }
}