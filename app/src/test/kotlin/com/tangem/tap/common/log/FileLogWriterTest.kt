package com.tangem.tap.common.log

import com.google.common.truth.Truth
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.utils.logging.Severity
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileLogWriterTest {

    private val appLogsStore: AppLogsStore = mockk(relaxUnitFun = true)
    private val writer = FileLogWriter(appLogsStore)

    // region isLoggable filter

    @Test
    fun `isLoggable returns true for Error severity`() {
        Truth.assertThat(writer.isLoggable(Severity.Error, "tag")).isTrue()
    }

    @Test
    fun `isLoggable returns true for Info severity`() {
        Truth.assertThat(writer.isLoggable(Severity.Info, "tag")).isTrue()
    }

    @Test
    fun `isLoggable returns false for Verbose severity`() {
        Truth.assertThat(writer.isLoggable(Severity.Verbose, "tag")).isFalse()
    }

    @Test
    fun `isLoggable returns false for Debug severity`() {
        Truth.assertThat(writer.isLoggable(Severity.Debug, "tag")).isFalse()
    }

    @Test
    fun `isLoggable returns false for Warn severity`() {
        Truth.assertThat(writer.isLoggable(Severity.Warn, "tag")).isFalse()
    }

    @Test
    fun `isLoggable returns false for Assert severity`() {
        Truth.assertThat(writer.isLoggable(Severity.Assert, "tag")).isFalse()
    }

    @Test
    fun `isLoggable result is independent of the tag value`() {
        Truth.assertThat(writer.isLoggable(Severity.Info, "")).isTrue()
        Truth.assertThat(writer.isLoggable(Severity.Info, "anything")).isTrue()
        Truth.assertThat(writer.isLoggable(Severity.Debug, "")).isFalse()
        Truth.assertThat(writer.isLoggable(Severity.Debug, "anything")).isFalse()
    }

    // endregion

    // region write delegation

    @Test
    fun `write forwards tag, message, throwable and shouldSanitize to AppLogsStore`() {
        // Arrange
        val throwable = IllegalStateException("boom")

        // Act
        writer.write(
            severity = Severity.Error,
            tag = "MyTag",
            message = "error happened",
            throwable = throwable,
            shouldSanitize = true,
        )

        // Assert
        verify(exactly = 1) {
            appLogsStore.saveLogMessage(
                tag = "MyTag",
                message = "error happened",
                throwable = throwable,
                shouldSanitize = true,
            )
        }
    }

    @Test
    fun `write forwards null throwable as null`() {
        // Act
        writer.write(
            severity = Severity.Info,
            tag = "Tag",
            message = "info",
            throwable = null,
            shouldSanitize = true,
        )

        // Assert
        verify(exactly = 1) {
            appLogsStore.saveLogMessage(
                tag = "Tag",
                message = "info",
                throwable = null,
                shouldSanitize = true,
            )
        }
    }

    @Test
    fun `write forwards shouldSanitize false to AppLogsStore so sanitizer is bypassed`() {
        // Act
        writer.write(
            severity = Severity.Info,
            tag = "Tag",
            message = "raw payload",
            throwable = null,
            shouldSanitize = false,
        )

        // Assert
        verify(exactly = 1) {
            appLogsStore.saveLogMessage(
                tag = "Tag",
                message = "raw payload",
                throwable = null,
                shouldSanitize = false,
            )
        }
    }

    @Test
    fun `write delegates regardless of severity (filtering is the caller's job)`() {
        // The contract: TangemLogger asks isLoggable first; if a caller bypasses that and
        // invokes write directly, the writer should still delegate to the store.
        Severity.entries.forEach { severity ->
            // Act
            writer.write(
                severity = severity,
                tag = "Tag",
                message = "msg-$severity",
                throwable = null,
                shouldSanitize = true,
            )

            // Assert
            verify(exactly = 1) {
                appLogsStore.saveLogMessage(
                    tag = "Tag",
                    message = "msg-$severity",
                    throwable = null,
                    shouldSanitize = true,
                )
            }
        }
    }

    // endregion
}