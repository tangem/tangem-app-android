package com.tangem.datasource.local.logs

import android.content.Context
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import timber.log.Timber
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
) {

    private val scope = CoroutineScope(
        context = SupervisorJob() + dispatchers.io +
            CoroutineExceptionHandler { _, error -> Timber.e("AppLogsStore.scope is failed $error") },
    )

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

    /** Save log [message] */
    fun saveLogMessage(tag: String, message: String) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(tag = tag, message)
        }
    }

    /** Save log that consists from [messages] */
    fun saveLogMessage(tag: String, vararg messages: String) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(tag = tag, *messages)
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

    fun deleteOldLogsFile() {
        val file = File(applicationContext.filesDir, LOG_FILE_NAME)

        if (file.exists()) file.delete()
    }

    fun deleteLastLogFile() {
        val file = File(applicationContext.filesDir, NEW_LOG_FILE_NAME)

        if (file.exists()) file.delete()
    }

    private fun writeMessage(tag: String, vararg messages: String) {
        BufferedWriter(FileWriter(logFile, true)).use { writer ->
            writer.append(formatter.print(DateTime.now()))
            writer.append(": $tag ")
            messages.map(LogsSanitizer::sanitize)
                .forEach(writer::append)
            writer.newLine()
        }
    }

    private fun createFileIfNotExist() {
        if (!logFile.exists()) {
            runCatching { logFile.createNewFile() }
                .onFailure(Timber::e)
        }
    }

    private fun launchWithLock(callback: () -> Unit) {
        scope.launch {
            mutex.withLock {
                runCatching { callback() }
                    .onFailure(Timber::e)
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

        const val LOG_FILE_NAME = "logs.txt"
        const val NEW_LOG_FILE_NAME = "app_logs.txt"
        // the only name that we allow to send as email to company addresses
        const val PERMITTED_FILE_NAME = "log.txt"
        const val PERMITTED_FILE_NAME_ZIP = "log.zip"
    }
}