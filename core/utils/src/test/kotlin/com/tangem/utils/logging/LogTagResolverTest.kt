package com.tangem.utils.logging

import com.google.common.truth.Truth
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogTagResolverTest {

    @AfterEach
    fun tearDown() {
        // Clean up shared TangemLogger state used in some cases
        TangemLogger.setLogWriters(emptyList())
    }

    @Test
    fun `resolveTag returns the simple class name of the direct caller`() {
        // Act
        val tag = LogTagResolver.resolveTag()

        // Assert
        Truth.assertThat(tag).isEqualTo("LogTagResolverTest")
    }

    @Test
    fun `resolveTag does not include package qualifier`() {
        // Act
        val tag = LogTagResolver.resolveTag()

        // Assert
        Truth.assertThat(tag).doesNotContain(".")
    }

    @Test
    fun `resolveTag never returns its own class name`() {
        // Act
        val tag = LogTagResolver.resolveTag()

        // Assert
        Truth.assertThat(tag).isNotEqualTo("LogTagResolver")
    }

    @Test
    fun `resolveTag skips TangemLogger frames when invoked through it`() {
        // Arrange
        var capturedTag: String? = null
        val writer = object : TangemLogger.LogWriter {
            override fun write(
                severity: Severity,
                tag: String,
                message: String,
                throwable: Throwable?,
                shouldSanitize: Boolean,
            ) {
                capturedTag = tag
            }
        }
        TangemLogger.setLogWriters(listOf(writer))

        // Act
        TangemLogger.d("via TangemLogger")

        // Assert — TangemLogger and LogTagResolver are filtered, leaving the test class
        Truth.assertThat(capturedTag).isEqualTo("LogTagResolverTest")
    }

    @Test
    fun `resolveTag is bypassed by TaggedLogger when an explicit tag is supplied`() {
        // Arrange
        var capturedTag: String? = null
        val writer = object : TangemLogger.LogWriter {
            override fun write(
                severity: Severity,
                tag: String,
                message: String,
                throwable: Throwable?,
                shouldSanitize: Boolean,
            ) {
                capturedTag = tag
            }
        }
        TangemLogger.setLogWriters(listOf(writer))

        // Act
        TangemLogger.withTag("ExplicitTag").d("hi")

        // Assert
        Truth.assertThat(capturedTag).isEqualTo("ExplicitTag")
    }

    @Test
    fun `resolveTag returns a non-empty string`() {
        // Act
        val tag = LogTagResolver.resolveTag()

        // Assert
        Truth.assertThat(tag).isNotEmpty()
    }
}