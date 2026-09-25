package com.tangem.data.walletconnect.network.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import org.junit.jupiter.api.Test

internal class LegacySdkHelperTest {

    @Test
    fun `GIVEN 0x hex message WHEN messageBytes THEN payload bytes are used`() {
        val actual = LegacySdkHelper.messageBytes("0x48656c6c6f") // "Hello"

        assertThat(actual).isEqualTo("Hello".toByteArray())
    }

    @Test
    fun `GIVEN plain text that looks like hex WHEN messageBytes THEN it is signed as text, not as bytes`() {
        val actual = LegacySdkHelper.messageBytes("deadbeef")

        assertThat(actual).isEqualTo("deadbeef".toByteArray(Charsets.UTF_8))
    }

    @Test
    fun `GIVEN non-ASCII plain text WHEN messageBytes THEN UTF-8 bytes are used, not an empty message`() {
        val actual = LegacySdkHelper.messageBytes("Привет 👋")

        assertThat(actual).isEqualTo("Привет 👋".toByteArray(Charsets.UTF_8))
        assertThat(actual).isNotEmpty()
    }

    @Test
    fun `GIVEN odd-length 0x string WHEN messageBytes THEN it is text, not a truncated byte string`() {
        val actual = LegacySdkHelper.messageBytes("0xabc")

        assertThat(actual).isEqualTo("0xabc".toByteArray(Charsets.UTF_8))
    }

    @Test
    fun `GIVEN hello WHEN createMessageData THEN matches the EIP-191 personal_sign hash`() {
        // keccak256("\u0019Ethereum Signed Message:\n5Hello"), computed with an independent Keccak implementation
        val actual = LegacySdkHelper.createMessageData("0x48656c6c6f").toHexString().lowercase()

        assertThat(actual).isEqualTo("aa744ba2ca576ec62ca0045eca00ad3917fdf7ffa34fbbae50828a5a69c1580e")
        // the same text sent as plain UTF-8 must hash identically
        assertThat(LegacySdkHelper.createMessageData("Hello").toHexString().lowercase()).isEqualTo(actual)
    }

    @Test
    fun `GIVEN hex-encoded UTF-8 message WHEN hexToAscii THEN non-ASCII text is shown`() {
        val hex = "0x" + "Привет 👋".toByteArray(Charsets.UTF_8).toHexString()

        assertThat(LegacySdkHelper.hexToAscii(hex)).isEqualTo("Привет 👋")
    }

    @Test
    fun `GIVEN binary payload WHEN hexToAscii THEN null so the raw params are shown instead`() {
        assertThat(LegacySdkHelper.hexToAscii("0x00ff10fe")).isNull()
    }

    @Test
    fun `GIVEN plain text message WHEN hexToAscii THEN it is shown as is`() {
        assertThat(LegacySdkHelper.hexToAscii("Sign in to Example")).isEqualTo("Sign in to Example")
    }
}
