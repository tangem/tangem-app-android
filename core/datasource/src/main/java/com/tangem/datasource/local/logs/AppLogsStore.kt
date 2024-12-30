package com.tangem.datasource.local.logs

import android.content.Context
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
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
    dispatchers: CoroutineDispatcherProvider,
) {

    private val scope = CoroutineScope(
        context = SupervisorJob() + dispatchers.io +
            CoroutineExceptionHandler { _, error -> Timber.e("AppLogsStore.scope is failed $error") },
    )
    private val mutex = Mutex()

    private val file = File(applicationContext.filesDir, NEW_LOG_FILE_NAME)

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
    fun getFile(): File? = if (file.exists()) file else null

    /** Save log [message] */
    fun saveLogMessage(message: String) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(message)
        }
    }

    /** Save log that consists from [messages] */
    fun saveLogMessage(vararg messages: String) {
        launchWithLock {
            createFileIfNotExist()

            writeMessage(*messages)
        }
    }

    /** Delete deprecated logs if file size exceeds [maxSize] */
    fun deleteDeprecatedLogs(maxSize: Int) {
        launchWithLock {
            if (file.exists() && file.length() > maxSize) {
                file.delete()
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

    private fun writeMessage(vararg messages: String) {
        BufferedWriter(FileWriter(file, true)).use { writer ->
            writer.append(formatter.print(DateTime.now()))
            writer.append(": ")
            messages.forEach(writer::append)
            writer.newLine()
        }
    }

    private fun createFileIfNotExist() {
        if (!file.exists()) {
            runCatching { file.createNewFile() }
                .onFailure(Timber::e)
        }
    }

    private fun launchWithLock(callback: () -> Unit) {
        scope.launch {
            mutex.withLock {
                runCatching {
                    callback()
                }.onFailure(Timber::e)
            }
        }
    }

    private companion object {
        const val LOG_FILE_NAME = "logs.txt"
        const val NEW_LOG_FILE_NAME = "app_logs.txt"
    }
}