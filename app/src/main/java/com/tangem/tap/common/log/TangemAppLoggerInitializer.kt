package com.tangem.tap.common.log

import android.os.Build
import android.util.Log
import co.touchlab.kermit.BaseLogger
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.orhanobut.logger.AndroidLogAdapter
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.wallet.BuildConfig
import timber.log.Timber
import java.util.regex.Pattern
import com.orhanobut.logger.Logger as PrettyLogger

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

    /** Initialize */
    fun initialize() {
        if (IS_LOG_ENABLED) {
            PrettyLogger.addLogAdapter(AndroidLogAdapter(TimberFormatStrategy()))
        }

        Timber.plant(tree = createTimberTree())
        Logger.setLogWriters(KermitLogWriter(::finalLogOutput))
    }

    private fun createTimberTree(): Timber.Tree {
        return object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                finalLogOutput(priority = priority, tag = tag, message = message, t = t)
            }
        }
    }

    private fun finalLogOutput(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (IS_LOG_ENABLED) {
            PrettyLogger.log(priority, tag, message, t)
        }

        if (PERMITTED_PRIORITY.contains(priority)) {
            appLogsStore.saveLogMessage(
                tag = tag ?: "TangemAppLogger",
                message = message,
            )
        }
    }

    @Suppress("BooleanPropertyNaming")
    private companion object {
        val IS_LOG_ENABLED: Boolean = BuildConfig.LOG_ENABLED
        val PERMITTED_PRIORITY = listOf(Log.ERROR, Log.INFO)
    }
}

private class KermitLogWriter(
    private val finalLogOutput: (priority: Int, tag: String?, message: String, t: Throwable?) -> Unit,
) : LogWriter() {

    private val fqcnIgnore = setOf(
        LogWriter::class.java.name,
        KermitLogWriter::class.java.name,
        BaseLogger::class.java.name,
        Logger::class.java.name,
    )

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val priority = when (severity) {
            Severity.Verbose -> PrettyLogger.VERBOSE
            Severity.Debug -> PrettyLogger.DEBUG
            Severity.Info -> PrettyLogger.INFO
            Severity.Warn -> PrettyLogger.WARN
            Severity.Error -> PrettyLogger.ERROR
            Severity.Assert -> PrettyLogger.ASSERT
        }

        val finalTag = if (tag != KERMIT_LOGGER_DEFAULT_TAG) {
            tag
        } else {
            /**
             * like in [Timber.DebugTree.tag]
             */
            @Suppress("UnnecessaryLet", "ThrowingExceptionsWithoutMessageOrCause")
            Throwable().stackTrace
                .first { it.className !in fqcnIgnore }
                .let(::createStackElementTag)
        }

        finalLogOutput(priority, finalTag, message, throwable)
    }

    /**
     * copy from [Timber.DebugTree.createStackElementTag]
     */
    @Suppress("MagicNumber")
    private fun createStackElementTag(element: StackTraceElement): String? {
        var tag = element.className.substringAfterLast('.')
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        // Tag length limit was removed in API 26.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    private companion object {
        private const val KERMIT_LOGGER_DEFAULT_TAG = ""

        /**
         * copy from [Timber.DebugTree.Companion]
         */
        private const val MAX_TAG_LENGTH = 23
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    }
}