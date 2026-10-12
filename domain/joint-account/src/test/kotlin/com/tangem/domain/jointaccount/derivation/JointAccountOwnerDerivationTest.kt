package com.tangem.domain.jointaccount.derivation

import arrow.core.getOrElse
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.network.Network
import org.junit.jupiter.api.Test
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.params.ParameterizedTest

@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
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

    /** Pins the reserved-node position inside the template: if the template shape changes, this fails first. */
    @ParameterizedTest
    @ProvideTestModels
    fun isJointAccountReservedPath(model: ReservedPathModel) {
        // Act
        val actual = isJointAccountReservedPath(model.rawPath)

        // Assert
        assertThat(actual).isEqualTo(model.isReserved)
    }

    internal data class ReservedPathModel(val rawPath: String, val isReserved: Boolean)

    private fun provideTestModels() = listOf(
        ReservedPathModel(rawPath = "m/44'/60'/888888'/0/0", isReserved = true),
        ReservedPathModel(rawPath = "m/44'/60'/888888'/1/5", isReserved = true),
        ReservedPathModel(rawPath = "m/44'/60'/1'/0/0", isReserved = false),
        ReservedPathModel(rawPath = "m/44'/60'/888888/0/0", isReserved = false),
        ReservedPathModel(rawPath = "m/44'/60'/0'/0/888888", isReserved = false),
        ReservedPathModel(rawPath = "not a path", isReserved = false),
    )

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
}