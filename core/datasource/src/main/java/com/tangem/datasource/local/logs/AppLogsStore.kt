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
            CoroutineExceptionHandler { _, error ->
                Timber.e("AppLogsStore.scope is failed $error")
                // Self-logging for scope failure
                runCatching {
                    Timber.e("AppLogsStore: Scope exception handler triggered: ${error.message}")
                }
            },
    )

    private val mutex = Mutex()
    private val zipMutex = Mutex()

    private val logFile by lazy {
        Timber.e("AppLogsStore: Initializing logFile with name: $PERMITTED_FILE_NAME")
        File(applicationContext.filesDir, PERMITTED_FILE_NAME)
    }

    private val logFileZip by lazy {
        Timber.e("AppLogsStore: Initializing logFileZip with name: $PERMITTED_FILE_NAME_ZIP")
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
    fun getFile(): File? {
        Timber.e("AppLogsStore: getFile() called")
        val exists = logFile.exists()
        Timber.e("AppLogsStore: logFile exists: $exists, path: ${logFile.absolutePath}")
        val result = if (exists) logFile else null
        Timber.e("AppLogsStore: getFile() returning: ${result?.name}")
        return result
    }

    suspend fun getZipFile(): File? {
        Timber.e("AppLogsStore: getZipFile() called")
        return zipMutex.withLock {
            Timber.e("AppLogsStore: getZipFile() acquired zipMutex lock")
            val logFileExists = logFile.exists()
            Timber.e("AppLogsStore: getZipFile() logFile exists: $logFileExists")

            if (logFileExists) {
                Timber.e("AppLogsStore: getZipFile() calling zip() with logFile: ${logFile.name}")
                val result = zip(filesToCompress = listOf(logFile), outputZipFile = logFileZip)
                Timber.e("AppLogsStore: getZipFile() zip result: ${result?.name}")
                result
            } else {
                Timber.e("AppLogsStore: getZipFile() returning null - logFile doesn't exist")
                null
            }
        }
    }

    /** Save log [message] */
    fun saveLogMessage(tag: String, message: String) {
        Timber.e("AppLogsStore: saveLogMessage() called with tag: '$tag', message length: ${message.length}")
        launchWithLock {
            Timber.e("AppLogsStore: saveLogMessage() inside launchWithLock for tag: '$tag'")
            createFileIfNotExist()
            writeMessage(tag = tag, message)
            Timber.e("AppLogsStore: saveLogMessage() completed for tag: '$tag'")
        }
    }

    /** Save log that consists from [messages] */
    fun saveLogMessage(tag: String, vararg messages: String) {
        Timber.e("AppLogsStore: saveLogMessage(varargs) called with tag: '$tag', messages count: ${messages.size}")
        launchWithLock {
            Timber.e("AppLogsStore: saveLogMessage(varargs) inside launchWithLock for tag: '$tag'")
            createFileIfNotExist()
            writeMessage(tag = tag, *messages)
            Timber.e("AppLogsStore: saveLogMessage(varargs) completed for tag: '$tag'")
        }
    }

    /** Delete deprecated logs if file size exceeds [maxSize] */
    fun deleteDeprecatedLogs(maxSize: Int) {
        Timber.e("AppLogsStore: deleteDeprecatedLogs() called with maxSize: $maxSize")
        launchWithLock {
            Timber.e("AppLogsStore: deleteDeprecatedLogs() inside launchWithLock")
            val exists = logFile.exists()
            val fileSize = if (exists) logFile.length() else 0
            Timber.e("AppLogsStore: deleteDeprecatedLogs() file exists: $exists, size: $fileSize")

            if (exists && fileSize > maxSize) {
                Timber.e("AppLogsStore: deleteDeprecatedLogs() deleting file - size $fileSize exceeds maxSize $maxSize")
                val deleted = logFile.delete()
                Timber.e("AppLogsStore: deleteDeprecatedLogs() file deletion result: $deleted")
            } else {
                Timber.e("AppLogsStore: deleteDeprecatedLogs() no deletion needed")
            }
        }
    }

    fun deleteOldLogsFile() {
        Timber.e("AppLogsStore: deleteOldLogsFile() called for file: $LOG_FILE_NAME")
        val file = File(applicationContext.filesDir, LOG_FILE_NAME)
        val exists = file.exists()
        Timber.e("AppLogsStore: deleteOldLogsFile() file exists: $exists")

        if (exists) {
            val deleted = file.delete()
            Timber.e("AppLogsStore: deleteOldLogsFile() deletion result: $deleted")
        }
    }

    fun deleteLastLogFile() {
        Timber.e("AppLogsStore: deleteLastLogFile() called for file: $NEW_LOG_FILE_NAME")
        val file = File(applicationContext.filesDir, NEW_LOG_FILE_NAME)
        val exists = file.exists()
        Timber.e("AppLogsStore: deleteLastLogFile() file exists: $exists")

        if (exists) {
            val deleted = file.delete()
            Timber.e("AppLogsStore: deleteLastLogFile() deletion result: $deleted")
        }
    }

    private fun writeMessage(tag: String, vararg messages: String) {
        Timber.e("AppLogsStore: writeMessage() called with tag: '$tag', messages count: ${messages.size}")
        try {
            BufferedWriter(FileWriter(logFile, true)).use { writer ->
                Timber.e("AppLogsStore: writeMessage() opened BufferedWriter")
                writer.append(formatter.print(DateTime.now()))
                writer.append(": $tag ")
                val sanitizedMessages = messages.map(LogsSanitizer::sanitize)
                Timber.e("AppLogsStore: writeMessage() sanitized ${sanitizedMessages.size} messages")
                sanitizedMessages.forEach(writer::append)
                writer.newLine()
                Timber.e("AppLogsStore: writeMessage() wrote message successfully")
            }
        } catch (e: Exception) {
            Timber.e("AppLogsStore: writeMessage() failed for tag '$tag': ${e.message}")
            throw e
        }
    }

    private fun createFileIfNotExist() {
        Timber.e("AppLogsStore: createFileIfNotExist() called")
        val exists = logFile.exists()
        Timber.e("AppLogsStore: createFileIfNotExist() file exists: $exists")

        if (!exists) {
            Timber.e("AppLogsStore: createFileIfNotExist() creating new file: ${logFile.absolutePath}")
            runCatching { logFile.createNewFile() }
                .onSuccess { created ->
                    Timber.e("AppLogsStore: createFileIfNotExist() file creation result: $created")
                }
                .onFailure { error ->
                    Timber.e("AppLogsStore: createFileIfNotExist() file creation failed: ${error.message}")
                    Timber.e(error)
                }
        }
    }

    private fun launchWithLock(callback: () -> Unit) {
        Timber.e("AppLogsStore: launchWithLock() called")
        scope.launch {
            Timber.e("AppLogsStore: launchWithLock() launched coroutine")
            mutex.withLock {
                Timber.e("AppLogsStore: launchWithLock() acquired mutex lock")
                runCatching {
                    callback()
                    Timber.e("AppLogsStore: launchWithLock() callback executed successfully")
                }
                .onFailure { error ->
                    Timber.e("AppLogsStore: launchWithLock() callback failed: ${error.message}")
                    Timber.e(error)
                }
            }
            Timber.e("AppLogsStore: launchWithLock() released mutex lock")
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun zip(filesToCompress: List<File>, outputZipFile: File): File? {
        Timber.e("AppLogsStore: zip() called with ${filesToCompress.size} files, output: ${outputZipFile.name}")
        return withContext(dispatchers.io) {
            Timber.e("AppLogsStore: zip() switched to IO context")

            val outputExists = outputZipFile.exists()
            Timber.e("AppLogsStore: zip() output file exists: $outputExists")

            if (outputExists && !outputZipFile.delete()) {
                Timber.e("AppLogsStore: zip() failed to delete existing output file")
                return@withContext null
            }

            val buffer = ByteArray(BUFFER_SIZE)
            Timber.e("AppLogsStore: zip() created buffer of size: $BUFFER_SIZE")

            try {
                FileOutputStream(outputZipFile).use { fos ->
                    Timber.e("AppLogsStore: zip() opened FileOutputStream")
                    ZipOutputStream(fos).use { zos ->
                        Timber.e("AppLogsStore: zip() opened ZipOutputStream")
                        filesToCompress.forEach { file ->
                            Timber.e("AppLogsStore: zip() processing file: ${file.name}, size: ${file.length()}")
                            FileInputStream(file).use { inStream ->
                                val ze = ZipEntry(file.name)
                                zos.putNextEntry(ze)
                                Timber.e("AppLogsStore: zip() created zip entry for: ${file.name}")

                                var len: Int
                                var totalBytes = 0
                                while (inStream.read(buffer).also { len = it } > 0) {
                                    zos.write(buffer, 0, len)
                                    totalBytes += len
                                }
                                Timber.e("AppLogsStore: zip() wrote $totalBytes bytes for file: ${file.name}")
                            }
                        }
                        zos.finish()
                        Timber.e("AppLogsStore: zip() finished zip stream")
                    }
                }
                Timber.e("AppLogsStore: zip() completed successfully, output size: ${outputZipFile.length()}")
                outputZipFile
            } catch (e: Exception) {
                Timber.e("AppLogsStore: zip() failed: ${e.message}")
                Timber.e(e)
                null
            }
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