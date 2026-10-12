package com.tangem.domain.sociallogin.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class RecoveryKeyTest {

    @Test
    fun `GIVEN 32 bytes WHEN created THEN key holds the value`() {
        // Arrange
        val bytes = ByteArray(RecoveryKey.SIZE_BYTES) { it.toByte() }

        // Act
        val key = RecoveryKey(bytes)

        // Assert
        assertThat(key.value).isEqualTo(bytes)
    }

    @Test
    fun `GIVEN wrong size WHEN created THEN fails`() {
        // Act
        val error = runCatching { RecoveryKey(ByteArray(size = 16)) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Recovery key must be 32 bytes long, but was 16")
    }

    @Test
    fun `GIVEN key WHEN wiped THEN value is zeroed`() {
        // Arrange
        val key = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })

        // Act
        key.wipe()

        // Assert
        assertThat(key.value).isEqualTo(ByteArray(RecoveryKey.SIZE_BYTES))
    }

    @Test
    fun `GIVEN equal content WHEN compared THEN keys are equal`() {
        // Arrange
        val key = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 1 })
        val sameKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 1 })
        val otherKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 2 })

        // Assert
        assertThat(key).isEqualTo(sameKey)
        assertThat(key.hashCode()).isEqualTo(sameKey.hashCode())
        assertThat(key).isNotEqualTo(otherKey)
    }

    @Test
    fun `GIVEN key WHEN printed THEN value is masked`() {
        // Arrange
        val key = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })

        // Assert
        assertThat(key.toString()).isEqualTo("RecoveryKey(***)")
    }
}