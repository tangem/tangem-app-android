package com.tangem.datasource.local.logs

import java.io.File

/**
 * Store for persisting app logs to a file the user can export.
 *
[REDACTED_AUTHOR]
 */
interface AppLogsStore {

    /** The current log file, or `null` if it does not exist yet. */
    fun getFile(): File?

    /** A zipped copy of the current log file, or `null` if there is nothing to zip. */
    suspend fun getZipFile(): File?

    /**
     * Append a log [message] under [tag].
     *
     * @param shouldSanitize when `true` (default), sensitive values in [message] are redacted before it
     * is written; pass `false` to persist the raw content. The optional [throwable]'s stack trace is
     * appended verbatim and is never sanitized.
     */
    fun saveLogMessage(tag: String, message: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)

    /** Append a log entry under [tag] composed of [messages]. */
    fun saveLogMessage(tag: String, vararg messages: String)

    /** Delete the log file if its size exceeds [maxSize] bytes. */
    fun deleteDeprecatedLogs(maxSize: Int)
}