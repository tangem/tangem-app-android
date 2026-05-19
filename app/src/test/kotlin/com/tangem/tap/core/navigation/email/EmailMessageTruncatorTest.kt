package com.tangem.tap.core.navigation.email

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailMessageTruncatorTest {

    private val truncator = EmailMessageTruncator()

    @Test
    fun `empty message returned as-is`() {
        val result = truncator.truncate("")

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `message under cap returned unchanged`() {
        val message = "small message"

        val result = truncator.truncate(message)

        assertThat(result).isEqualTo(message)
    }

    @Test
    fun `message exactly at cap returned unchanged`() {
        val message = "a".repeat(MAX_MESSAGE_BYTES)

        val result = truncator.truncate(message)

        assertThat(result).isEqualTo(message)
    }

    @Test
    fun `message over cap is truncated to fit within cap in bytes`() {
        val message = "a".repeat(MAX_MESSAGE_BYTES + 1_000)

        val result = truncator.truncate(message)

        assertThat(result.toByteArray(Charsets.UTF_8).size).isAtMost(MAX_MESSAGE_BYTES)
    }

    @Test
    fun `truncated message preserves the head of the original`() {
        val head = "HEAD_MARKER_" + "x".repeat(100)
        val tail = "y".repeat(MAX_MESSAGE_BYTES)
        val message = head + tail

        val result = truncator.truncate(message)

        assertThat(result).startsWith(head)
    }

    @Test
    fun `truncated message ends with the truncation suffix`() {
        val message = "a".repeat(MAX_MESSAGE_BYTES + 1_000)

        val result = truncator.truncate(message)

        assertThat(result).contains("[truncated, original ${message.length} bytes]")
    }

    @Test
    fun `truncation suffix reports original byte length not character length`() {
        // Each emoji is 4 bytes in UTF-8.
        val emoji = "😀" // 😀
        val message = emoji.repeat(MAX_MESSAGE_BYTES / 4 + 10)
        val originalBytes = message.toByteArray(Charsets.UTF_8).size

        val result = truncator.truncate(message)

        assertThat(result).contains("[truncated, original $originalBytes bytes]")
    }

    @Test
    fun `multi-byte UTF-8 boundary stays within cap and produces valid output`() {
        // Build a message where the cap falls inside a multi-byte char.
        val emoji = "😀" // 😀, 4 bytes in UTF-8
        val message = emoji.repeat(MAX_MESSAGE_BYTES) // Way over cap.

        val result = truncator.truncate(message)

        // Partial trailing char is dropped (not replaced with U+FFFD which is 3 bytes and would
        // push the result over the cap), so the result must stay within the cap and survive a
        // UTF-8 round-trip.
        val roundTripped = String(result.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
        assertThat(roundTripped).isEqualTo(result)
        assertThat(result.toByteArray(Charsets.UTF_8).size).isAtMost(MAX_MESSAGE_BYTES)
    }

    private companion object {
        // Mirror the constant inside EmailMessageTruncator. Keep in sync if it changes there.
        const val MAX_MESSAGE_BYTES = 20_000
    }
}