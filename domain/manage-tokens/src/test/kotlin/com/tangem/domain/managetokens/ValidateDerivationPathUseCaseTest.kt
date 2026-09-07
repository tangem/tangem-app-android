package com.tangem.domain.managetokens

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.managetokens.model.exceptoin.DerivationPathValidationException
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The 888888' node is reserved for joint account owner keys: a custom currency under it would collide with a
 * joint currency in every store keyed by the derivation path.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateDerivationPathUseCaseTest {

    private val repository: CustomTokensRepository = mockk {
        every { createDerivationPath(any()) } answers { Network.DerivationPath.Custom(value = firstArg()) }
    }
    private val useCase = ValidateDerivationPathUseCase(repository)

    @Test
    fun `GIVEN regular custom path WHEN invoke THEN path is returned`() {
        // Act
        val actual = useCase("m/44'/60'/1'/0/0")

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(Network.DerivationPath.Custom("m/44'/60'/1'/0/0"))
    }

    @Test
    fun `GIVEN path under the joint node WHEN invoke THEN reserved error`() {
        // Act
        val exactOwnerPath = useCase("m/44'/60'/888888'/0/2")
        val anotherTail = useCase("m/44'/60'/888888'/1/7")

        // Assert
        assertThat(exactOwnerPath.leftOrNull())
            .isEqualTo(DerivationPathValidationException.ReservedForJointAccounts)
        assertThat(anotherTail.leftOrNull())
            .isEqualTo(DerivationPathValidationException.ReservedForJointAccounts)
    }

    @Test
    fun `GIVEN unparseable path WHEN invoke THEN invalid error, not reserved`() {
        // Arrange
        io.mockk.every { repository.createDerivationPath("m/44'/60'/888888'/oops") }
            .throws(IllegalArgumentException())

        // Act
        val actual = useCase("m/44'/60'/888888'/oops")

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(DerivationPathValidationException.Invalid)
    }

    @Test
    fun `GIVEN non-hardened or non-account 888888 WHEN invoke THEN path is allowed`() {
        // Act
        val nonHardened = useCase("m/44'/60'/888888/0/0")
        val asFinalIndex = useCase("m/44'/60'/0'/0/888888")

        // Assert
        assertThat(nonHardened.isRight()).isTrue()
        assertThat(asFinalIndex.isRight()).isTrue()
    }

    @Test
    fun `GIVEN empty value WHEN invoke THEN empty error`() {
        // Act
        val actual = useCase("")

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(DerivationPathValidationException.Empty)
    }
}