package com.tangem.domain.jointaccount.derivation

import arrow.core.getOrElse
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.network.Network
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

    @Test
    fun `GIVEN owner key index WHEN toNetworkDerivationPath THEN joint path at that index`() {
        // Arrange
        val index = OwnerKeyIndex(value = 7).getOrElse { error(it) }

        // Act
        val actual = index.toNetworkDerivationPath()

        // Assert
        assertThat(actual).isEqualTo(Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/7"))
    }

    @Test
    fun `GIVEN owner key path WHEN isJointAccountOwnerDerivationPath THEN true`() {
        // Act
        val actual = isJointAccountOwnerDerivationPath(rawPath = "m/44'/60'/888888'/0/19")

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN path of a wallet's own account WHEN isJointAccountOwnerDerivationPath THEN false`() {
        // Act
        val actual = isJointAccountOwnerDerivationPath(rawPath = "m/44'/60'/0'/0/0")

        // Assert
        assertThat(actual).isFalse()
    }
}