package com.tangem.utils.logging

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Application-level logger
 */
object TangemLogger : BaseLogger {

    private val logWriters = CopyOnWriteArrayList<LogWriter>()

    fun setLogWriters(writers: List<LogWriter>) {
        logWriters.clear()
        logWriters.addAll(writers)
    }

    fun addLogWriter(writer: LogWriter) {
        logWriters.add(writer)
    }

    override fun v(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
        write(
            severity = Severity.Verbose,
            tag = null,
            message = messageString,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }

    override fun d(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
        write(
            severity = Severity.Debug,
            tag = null,
            message = messageString,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }

    override fun i(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
        write(
            severity = Severity.Info,
            tag = null,
            message = messageString,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }

    override fun w(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
        write(
            severity = Severity.Warn,
            tag = null,
            message = messageString,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }

    override fun e(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
        write(
            severity = Severity.Error,
            tag = null,
            message = messageString,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }

    override fun a(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
        write(
            severity = Severity.Assert,
            tag = null,
            message = messageString,
            throwable = throwable,
            shouldSanitize = shouldSanitize,
        )
    }

    fun withTag(tag: String): TaggedLogger = TaggedLogger(tag)

    private fun write(
        severity: Severity,
        tag: String?,
        message: String,
        throwable: Throwable?,
        shouldSanitize: Boolean,
    ) {
        val resolvedTag = tag ?: LogTagResolver.resolveTag()
        logWriters.forEach { writer ->
            if (writer.isLoggable(severity, resolvedTag)) {
                writer.write(
                    severity = severity,
                    tag = resolvedTag,
                    message = message,
                    throwable = throwable,
                    shouldSanitize = shouldSanitize,
                )
            }
        }
    }

    class TaggedLogger internal constructor(private val tag: String) : BaseLogger {

        override fun v(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
            write(
                severity = Severity.Verbose,
                tag = tag,
                message = messageString,
                throwable = throwable,
                shouldSanitize = shouldSanitize,
            )
        }

        override fun d(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
            write(
                severity = Severity.Debug,
                tag = tag,
                message = messageString,
                throwable = throwable,
                shouldSanitize = shouldSanitize,
            )
        }

        override fun i(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
            write(
                severity = Severity.Info,
                tag = tag,
                message = messageString,
                throwable = throwable,
                shouldSanitize = shouldSanitize,
            )
        }

        override fun w(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
            write(
                severity = Severity.Warn,
                tag = tag,
                message = messageString,
                throwable = throwable,
                shouldSanitize = shouldSanitize,
            )
        }

        override fun e(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
            write(
                severity = Severity.Error,
                tag = tag,
                message = messageString,
                throwable = throwable,
                shouldSanitize = shouldSanitize,
            )
        }

        override fun a(messageString: String, throwable: Throwable?, shouldSanitize: Boolean) {
            write(
                severity = Severity.Assert,
                tag = tag,
                message = messageString,
                throwable = throwable,
                shouldSanitize = shouldSanitize,
            )
        }
    }

    interface LogWriter {

        fun isLoggable(severity: Severity, tag: String): Boolean = true

        fun write(severity: Severity, tag: String, message: String, throwable: Throwable?, shouldSanitize: Boolean)
    }
}