package com.tangem.tap.common.log

import android.util.Log
import com.tangem.utils.logging.Severity
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogcatLogWriterTest {

    private val writer = LogcatLogWriter()

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.getStackTraceString(any()) } returns "STACK"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // region Severity → Android priority mapping

    @Test
    fun `Verbose severity maps to Log VERBOSE priority`() {
        // Act
        writer.write(Severity.Verbose, "Tag", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.VERBOSE, "Tag", any()) }
    }

    @Test
    fun `Debug severity maps to Log DEBUG priority`() {
        // Act
        writer.write(Severity.Debug, "Tag", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.DEBUG, "Tag", any()) }
    }

    @Test
    fun `Info severity maps to Log INFO priority`() {
        // Act
        writer.write(Severity.Info, "Tag", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.INFO, "Tag", any()) }
    }

    @Test
    fun `Warn severity maps to Log WARN priority`() {
        // Act
        writer.write(Severity.Warn, "Tag", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.WARN, "Tag", any()) }
    }

    @Test
    fun `Error severity maps to Log ERROR priority`() {
        // Act
        writer.write(Severity.Error, "Tag", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.ERROR, "Tag", any()) }
    }

    @Test
    fun `Assert severity maps to Log ASSERT priority`() {
        // Act
        writer.write(Severity.Assert, "Tag", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.ASSERT, "Tag", any()) }
    }

    // endregion

    // region Box layout

    @Test
    fun `single-line message is wrapped between top and bottom borders`() {
        // Act
        writer.write(Severity.Info, "Tag", "hello", throwable = null, shouldSanitize = true)

        // Assert
        verifySequence {
            Log.println(Log.INFO, "Tag", match<String> { it.startsWith("┌") })
            Log.println(Log.INFO, "Tag", "│ hello")
            Log.println(Log.INFO, "Tag", match<String> { it.startsWith("└") })
        }
    }

    @Test
    fun `each line of a multi-line message is printed as a separate logcat entry`() {
        // Arrange
        val sep = System.lineSeparator()
        val message = "first${sep}second${sep}third"

        // Act
        writer.write(Severity.Debug, "Tag", message, throwable = null, shouldSanitize = true)

        // Assert
        verifySequence {
            Log.println(Log.DEBUG, "Tag", match<String> { it.startsWith("┌") })
            Log.println(Log.DEBUG, "Tag", "│ first")
            Log.println(Log.DEBUG, "Tag", "│ second")
            Log.println(Log.DEBUG, "Tag", "│ third")
            Log.println(Log.DEBUG, "Tag", match<String> { it.startsWith("└") })
        }
    }

    // endregion

    // region Throwable handling

    @Test
    fun `throwable is rendered via Log getStackTraceString`() {
        // Arrange
        val throwable = RuntimeException("boom")
        every { Log.getStackTraceString(throwable) } returns "STACK"

        // Act
        writer.write(Severity.Error, "Tag", "fail", throwable = throwable, shouldSanitize = true)

        // Assert
        verify(exactly = 1) { Log.getStackTraceString(throwable) }
    }

    @Test
    fun `null throwable does not invoke getStackTraceString`() {
        // Act
        writer.write(Severity.Info, "Tag", "no throwable", throwable = null, shouldSanitize = true)

        // Assert
        verify(exactly = 0) { Log.getStackTraceString(any()) }
    }

    // endregion

    // region Chunking of long messages

    @Test
    fun `message under CHUNK_SIZE bytes produces a single content line`() {
        // Arrange
        val message = "a".repeat(3999)

        // Act
        writer.write(Severity.Info, "Tag", message, throwable = null, shouldSanitize = true)

        // Assert — top border + 1 content line + bottom border
        verify(exactly = 3) { Log.println(Log.INFO, "Tag", any()) }
    }

    @Test
    fun `message exceeding CHUNK_SIZE bytes is split into multiple chunks`() {
        // Arrange — 9000 ASCII bytes → chunks of 4000 + 4000 + 1000 = 3 chunks
        val message = "a".repeat(9000)

        // Act
        writer.write(Severity.Info, "Tag", message, throwable = null, shouldSanitize = true)

        // Assert — top border + 3 content lines + bottom border
        verify(exactly = 5) { Log.println(Log.INFO, "Tag", any()) }
    }

    // endregion

    // region Tag truncation

    @Test
    fun `tag longer than 23 chars is truncated on legacy Android API stub`() {
        // Arrange — in the unit-test Android stub, Build.VERSION.SDK_INT == 0,
        // triggering the legacy truncation path.
        val longTag = "a".repeat(50)

        // Act
        writer.write(Severity.Info, longTag, "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.INFO, "a".repeat(23), any()) }
    }

    @Test
    fun `tag of MAX_TAG_LENGTH chars is not truncated`() {
        // Arrange
        val tag = "a".repeat(23)

        // Act
        writer.write(Severity.Info, tag, "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.INFO, "a".repeat(23), any()) }
    }

    @Test
    fun `short tag is forwarded verbatim`() {
        // Act
        writer.write(Severity.Info, "Short", "msg", throwable = null, shouldSanitize = true)

        // Assert
        verify { Log.println(Log.INFO, "Short", any()) }
    }

    // endregion
}