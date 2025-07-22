package com.tangem.utils.extensions

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ByteArrayExtTest {

    @Test
    fun `toHexString converts byte array to hexadecimal string`() {
        // Arrange
        val byteArray = byteArrayOf(0x0F, 0xA5.toByte(), 0xFF.toByte())

        // Act
        val actual = byteArray.toHexString()

        // Assert
        val expected = "0FA5FF"
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `hexToBytes converts hexadecimal string to byte array`() {
        // Arrange
        val hexString = "0FA5FF"

        // Act
        val actual = hexString.hexToBytes()

        // Assert
        val expected = byteArrayOf(0x0F, 0xA5.toByte(), 0xFF.toByte())
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `hexToBytes handles hex string with prefix`() {
        // Arrange
        val hexString = "0x0FA5FF"

        // Act
        val actual = hexString.hexToBytes()

        // Assert
        val expected = byteArrayOf(0x0F, 0xA5.toByte(), 0xFF.toByte())
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `hexToBytes throws exception for invalid hex string`() {
        // Arrange
        val invalidHexString = "GHIJKL"

        // Act
        val actual = runCatching { invalidHexString.hexToBytes() }.exceptionOrNull()

        // Assert
        val expected = NumberFormatException()
        Truth.assertThat(actual).isInstanceOf(expected::class.java)
        Truth.assertThat(actual).hasMessageThat().isEqualTo("For input string: \"GH\" under radix 16")
    }

    @Test
    fun `hexToBytes handles empty string`() {
        // Arrange
        val emptyHexString = ""

        // Act
        val actual = emptyHexString.hexToBytes()

        // Assert
        Truth.assertThat(actual).isEqualTo(byteArrayOf())
    }

    @Test
    fun `hexToBytesOrNull returns byte array for valid hex string`() {
        // Arrange
        val hexString = "0FA5FF"

        // Act
        val actual = hexString.hexToBytesOrNull()

        // Assert
        val expected = byteArrayOf(0x0F, 0xA5.toByte(), 0xFF.toByte())
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `hexToBytesOrNull returns null for invalid hex string`() {
        // Arrange
        val invalidHexString = "GHIJKL"

        // Act
        val actual = invalidHexString.hexToBytesOrNull()

        // Assert
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun hexToBytesOrNullHandlesEmptyString() {
        // Arrange
        val emptyHexString = ""

        // Act
        val actual = emptyHexString.hexToBytesOrNull()

        // Assert
        val expected = byteArrayOf()
        Truth.assertThat(actual).isEqualTo(expected)
    }
}