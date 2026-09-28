package com.tangem.datasource.local.logs

import android.content.Context
import com.tangem.datasource.BuildConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Store for saving app logs
 *
 * @property applicationContext application context
 * @param dispatchers           coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultAppLogsStore @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scope: AppCoroutineScope,
) : AppLogsStore {

    private val mutex = Mutex()
    private val zipMutex = Mutex()

    private val logFile by lazy {
        File(applicationContext.filesDir, PERMITTED_FILE_NAME)
    }

    private val logFileZip by lazy {
        File(applicationContext.filesDir, PERMITTED_FILE_NAME_ZIP)
    }

    // Separate from logFileZip: the archive with info carries wallet data, and a later logs-only request
    // (e.g. the support email) must not overwrite it while its consumer is still reading it
    private val infoLogsDir by lazy {
        File(applicationContext.filesDir, INFO_LOGS_DIR_NAME)
    }

    private val infoLogsFile by lazy {
        File(infoLogsDir, INFO_LOGS_FILE_NAME)
    }

    private val logWithInfoFileZip by lazy {
        File(infoLogsDir, PERMITTED_FILE_NAME_ZIP)
    }

    private val formatter = DateTimeFormatterBuilder()
        .appendDayOfMonth(2)
        .appendLiteral('.')
        .appendMonthOfYear(2)
        .appendLiteral(' ')
        .appendHourOfDay(1)
        .appendLiteral(':')
        .appendMinuteOfHour(2)
        .appendLiteral(':')
        .appendSecondOfMinute(2)
        .appendLiteral('.')
        .appendMillisOfSecond(3)
        .toFormatter()

    /** Get log file */
    override fun getFile(): File? = if (logFile.exists()) logFile else null

    override suspend fun getZipFile(infoLogs: String?): File? {
        return zipMutex.withLock {
            withContext(dispatchers.io) {
                if (!logFile.exists() && infoLogs == null) return@withContext null

                try {
                    val filesToCompress = buildList {
                        if (logFile.exists()) add(logFile)
                        if (infoLogs != null) add(writeInfoLogsFile(infoLogs))
                    }
                    val outputZipFile = if (infoLogs != null) logWithInfoFileZip else logFileZip

                    zip(filesToCompress = filesToCompress, outputZipFile = outputZipFile)
                } finally {
                    infoLogsFile.delete()
                }
            }
        }
    }

    private fun writeInfoLogsFile(infoLogs: String): File {
        infoLogsDir.mkdirs()
        return infoLogsFile.apply { writeText(infoLogs) }
    }

    /**
     * Save log [message]. Pass [shouldSanitize] = false to bypass [LogsSanitizer].
     * Sanitization is also bypassed entirely when [BuildConfig.LOG_ENABLED] is true,
     * so builds with logging enabled can expose raw values for testing.
     * The optional [throwable]'s stack trace is appended verbatim (never sanitized),
     * since stack traces routinely contain hex-like sequences that the sanitizer would
     * otherwise destroy.
     */
    override fun saveLogMessage(tag: String, message: String, throwable: Throwable?, shouldSanitize: Boolean) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(tag = tag, shouldSanitize = shouldSanitize, throwable = throwable, message)
        }
    }

    /** Save log that consists from [messages] */
    override fun saveLogMessage(tag: String, vararg messages: String) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(tag = tag, shouldSanitize = true, throwable = null, messages = messages)
        }
    }

    /** Delete deprecated logs if file size exceeds [maxSize] */
    override fun deleteDeprecatedLogs(maxSize: Int) {
        launchWithLock {
            if (logFile.exists() && logFile.length() > maxSize) {
                logFile.delete()
            }
        }
    }

    private fun writeMessage(tag: String, shouldSanitize: Boolean, throwable: Throwable?, vararg messages: String) {
        BufferedWriter(FileWriter(logFile, true)).use { writer ->
            writer.append(formatter.print(DateTime.now()))
            writer.append(": $tag ")
            val processed = if (shouldSanitize && !BuildConfig.LOG_ENABLED) {
                messages.map(LogsSanitizer::sanitize)
            } else {
                messages.toList()
            }
            processed.forEach(writer::append)
            if (throwable != null) {
                writer.newLine()
                writer.append(throwable.stackTraceToString().trimEnd())
            }
            writer.newLine()
        }
    }

    private fun createFileIfNotExist() {
        if (!logFile.exists()) {
            runCatching { logFile.createNewFile() }
                .onFailure { TangemLogger.e("Error", it) }
        }
    }

    private fun launchWithLock(callback: () -> Unit) {
        scope.launch {
            mutex.withLock {
                runCatching { callback() }
                    .onFailure { TangemLogger.e("Error", it) }
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun zip(filesToCompress: List<File>, outputZipFile: File): File? {
        return withContext(dispatchers.io) {
            if (outputZipFile.exists() && !outputZipFile.delete()) {
                return@withContext null
            }

            val buffer = ByteArray(BUFFER_SIZE)

            FileOutputStream(outputZipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    filesToCompress.forEach { file ->
                        FileInputStream(file).use { inStream ->
                            val ze = ZipEntry(file.name)
                            zos.putNextEntry(ze)
                            var len: Int
                            while (inStream.read(buffer).also { len = it } > 0) {
                                zos.write(buffer, 0, len)
                            }
                        }
                    }
                    zos.finish() // Ensures the zip output is finalized
                }
            }
            outputZipFile
        }
    }

    private companion object {
        const val BUFFER_SIZE = 1024

        // the only name that we allow to send as email to company addresses
        const val PERMITTED_FILE_NAME = "log.txt"
        const val PERMITTED_FILE_NAME_ZIP = "log.zip"
        const val INFO_LOGS_DIR_NAME = "info_logs"
        const val INFO_LOGS_FILE_NAME = "infoLogs.txt"
    }
}