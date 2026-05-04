package com.tangem.datasource.local.logs

import android.content.Context
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
import javax.inject.Singleton

/**
 * Store for saving app logs
 *
 * @property applicationContext application context
 * @param dispatchers           coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
@Singleton
class AppLogsStore @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scope: AppCoroutineScope,
) {

    private val mutex = Mutex()
    private val zipMutex = Mutex()

    private val logFile by lazy {
        File(applicationContext.filesDir, PERMITTED_FILE_NAME)
    }

    private val logFileZip by lazy {
        File(applicationContext.filesDir, PERMITTED_FILE_NAME_ZIP)
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
    fun getFile(): File? = if (logFile.exists()) logFile else null

    suspend fun getZipFile(): File? {
        return zipMutex.withLock {
            if (logFile.exists()) {
                zip(filesToCompress = listOf(logFile), outputZipFile = logFileZip)
            } else {
                null
            }
        }
    }

    /**
     * Save log [message]. Pass [shouldSanitize] = false to bypass [LogsSanitizer].
     * The optional [throwable]'s stack trace is appended verbatim (never sanitized),
     * since stack traces routinely contain hex-like sequences that the sanitizer would
     * otherwise destroy.
     */
    fun saveLogMessage(tag: String, message: String, throwable: Throwable? = null, shouldSanitize: Boolean = true) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(tag = tag, shouldSanitize = shouldSanitize, throwable = throwable, message)
        }
    }

    /** Save log that consists from [messages] */
    fun saveLogMessage(tag: String, vararg messages: String) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(tag = tag, shouldSanitize = true, throwable = null, messages = messages)
        }
    }

    /** Delete deprecated logs if file size exceeds [maxSize] */
    fun deleteDeprecatedLogs(maxSize: Int) {
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
            val processed = if (shouldSanitize) messages.map(LogsSanitizer::sanitize) else messages.toList()
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
    }
}