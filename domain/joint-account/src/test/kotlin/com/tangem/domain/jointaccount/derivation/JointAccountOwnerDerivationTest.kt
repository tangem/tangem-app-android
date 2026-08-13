package com.tangem.domain.jointaccount.derivation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class JointAccountOwnerDerivationTest {

    @Test
    fun `GIVEN owner index WHEN build path THEN matches the cross-platform contract byte for byte`() {
        // Act
        val zero = jointAccountOwnerDerivationPath(index = 0)
        val nineteen = jointAccountOwnerDerivationPath(index = 19)

        // Assert
        assertThat(zero.rawPath).isEqualTo("m/44'/60'/888888'/0/0")
        assertThat(nineteen.rawPath).isEqualTo("m/44'/60'/888888'/0/19")
    }

    @Test
    fun `GIVEN negative index WHEN build path THEN throws`() {
        // Act
        val exception = runCatching { jointAccountOwnerDerivationPath(index = -1) }.exceptionOrNull()

        // Assert
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}