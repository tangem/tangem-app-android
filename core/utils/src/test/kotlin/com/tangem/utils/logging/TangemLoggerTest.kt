package com.tangem.utils.logging

import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TangemLoggerTest {

    private lateinit var writer: TangemLogger.LogWriter

    @BeforeEach
    fun setUp() {
        writer = mockk(relaxed = true)
        every { writer.isLoggable(any(), any()) } returns true
        TangemLogger.setLogWriters(listOf(writer))
    }

    @AfterEach
    fun tearDown() {
        // Reset singleton state to avoid cross-test pollution
        TangemLogger.setLogWriters(emptyList())
    }

    // region Severity dispatch

    @Test
    fun `v dispatches Verbose severity to writer`() {
        // Act
        TangemLogger.v("verbose message")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Verbose, any(), "verbose message", null, true)
        }
    }

    @Test
    fun `d dispatches Debug severity to writer`() {
        // Act
        TangemLogger.d("debug message")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Debug, any(), "debug message", null, true)
        }
    }

    @Test
    fun `i dispatches Info severity to writer`() {
        // Act
        TangemLogger.i("info message")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Info, any(), "info message", null, true)
        }
    }

    @Test
    fun `w dispatches Warn severity to writer`() {
        // Act
        TangemLogger.w("warn message")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Warn, any(), "warn message", null, true)
        }
    }

    @Test
    fun `e dispatches Error severity to writer`() {
        // Act
        TangemLogger.e("error message")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Error, any(), "error message", null, true)
        }
    }

    @Test
    fun `a dispatches Assert severity to writer`() {
        // Act
        TangemLogger.a("assert message")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Assert, any(), "assert message", null, true)
        }
    }

    // endregion

    // region Throwable & shouldSanitize propagation

    @Test
    fun `throwable parameter is forwarded to writer`() {
        // Arrange
        val throwable = IllegalStateException("boom")

        // Act
        TangemLogger.e("error", throwable)

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Error, any(), "error", throwable, true)
        }
    }

    @Test
    fun `shouldSanitize flag is forwarded to writer`() {
        // Act
        TangemLogger.w("not checked", shouldSanitize = false)

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Warn, any(), "not checked", null, false)
        }
    }

    // endregion

    // region setLogWriters / addLogWriter

    @Test
    fun `setLogWriters replaces previously registered writers`() {
        // Arrange
        val previous: TangemLogger.LogWriter = mockk(relaxed = true)
        every { previous.isLoggable(any(), any()) } returns true
        val replacement: TangemLogger.LogWriter = mockk(relaxed = true)
        every { replacement.isLoggable(any(), any()) } returns true

        TangemLogger.setLogWriters(listOf(previous))
        TangemLogger.setLogWriters(listOf(replacement))

        // Act
        TangemLogger.i("after replace")

        // Assert
        verify(exactly = 0) { previous.write(any(), any(), any(), any(), any()) }
        verify(exactly = 1) {
            replacement.write(Severity.Info, any(), "after replace", null, true)
        }
    }

    @Test
    fun `addLogWriter appends without removing existing writers`() {
        // Arrange
        val first: TangemLogger.LogWriter = mockk(relaxed = true)
        every { first.isLoggable(any(), any()) } returns true
        val second: TangemLogger.LogWriter = mockk(relaxed = true)
        every { second.isLoggable(any(), any()) } returns true

        TangemLogger.setLogWriters(listOf(first))
        TangemLogger.addLogWriter(second)

        // Act
        TangemLogger.d("broadcast")

        // Assert
        verify(exactly = 1) { first.write(Severity.Debug, any(), "broadcast", null, true) }
        verify(exactly = 1) { second.write(Severity.Debug, any(), "broadcast", null, true) }
    }

    @Test
    fun `setLogWriters with empty list silences all output`() {
        // Arrange
        TangemLogger.setLogWriters(emptyList())

        // Act
        TangemLogger.i("nobody listening")

        // Assert
        verify(exactly = 0) { writer.write(any(), any(), any(), any(), any()) }
    }

    // endregion

    // region isLoggable filtering

    @Test
    fun `write is skipped when isLoggable returns false`() {
        // Arrange
        every { writer.isLoggable(any(), any()) } returns false

        // Act
        TangemLogger.w("filtered out")

        // Assert
        verify(exactly = 1) { writer.isLoggable(Severity.Warn, any()) }
        verify(exactly = 0) { writer.write(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `each writer is filtered independently by its own isLoggable`() {
        // Arrange
        val accepting: TangemLogger.LogWriter = mockk(relaxed = true)
        every { accepting.isLoggable(any(), any()) } returns true
        val rejecting: TangemLogger.LogWriter = mockk(relaxed = true)
        every { rejecting.isLoggable(any(), any()) } returns false

        TangemLogger.setLogWriters(listOf(accepting, rejecting))

        // Act
        TangemLogger.i("partial")

        // Assert
        verify(exactly = 1) { accepting.write(Severity.Info, any(), "partial", null, true) }
        verify(exactly = 0) { rejecting.write(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `LogWriter isLoggable defaults to true`() {
        // Arrange
        val realWriter = object : TangemLogger.LogWriter {
            override fun write(
                severity: Severity,
                tag: String,
                message: String,
                throwable: Throwable?,
                shouldSanitize: Boolean,
            ) = Unit
        }

        // Act + Assert
        Severity.entries.forEach { severity ->
            Truth.assertThat(realWriter.isLoggable(severity, "anyTag")).isTrue()
        }
    }

    // endregion

    // region Tag resolution

    @Test
    fun `resolved tag falls back to caller class name when no tag is provided`() {
        // Act
        TangemLogger.d("no tag")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Debug, "TangemLoggerTest", "no tag", null, true)
        }
    }

    @Test
    fun `withTag returns a TaggedLogger that uses the supplied tag`() {
        // Arrange
        val tagged = TangemLogger.withTag("MyFeature")

        // Act
        tagged.i("hello")

        // Assert
        verify(exactly = 1) {
            writer.write(Severity.Info, "MyFeature", "hello", null, true)
        }
    }

    // endregion

    // region TaggedLogger

    @Test
    fun `TaggedLogger dispatches each severity with its tag, throwable and shouldSanitize flag`() {
        // Arrange
        val tagged = TangemLogger.withTag("Tag")
        val throwable = RuntimeException("oops")

        // Act
        tagged.v("v")
        tagged.d("d")
        tagged.i("i")
        tagged.w("w")
        tagged.e("e", throwable)
        tagged.a("a", shouldSanitize = false)

        // Assert
        verifyOrder {
            writer.write(Severity.Verbose, "Tag", "v", null, true)
            writer.write(Severity.Debug, "Tag", "d", null, true)
            writer.write(Severity.Info, "Tag", "i", null, true)
            writer.write(Severity.Warn, "Tag", "w", null, true)
            writer.write(Severity.Error, "Tag", "e", throwable, true)
            writer.write(Severity.Assert, "Tag", "a", null, false)
        }
    }

    @Test
    fun `TaggedLogger respects writer isLoggable filtering`() {
        // Arrange
        every { writer.isLoggable(any(), any()) } returns false
        val tagged = TangemLogger.withTag("Filtered")

        // Act
        tagged.e("ignored")

        // Assert
        verify(exactly = 1) { writer.isLoggable(Severity.Error, "Filtered") }
        verify(exactly = 0) { writer.write(any(), any(), any(), any(), any()) }
    }

    // endregion
}