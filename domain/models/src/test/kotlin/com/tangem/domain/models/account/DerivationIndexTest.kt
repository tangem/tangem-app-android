package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DerivationIndexTest {

    @Test
    fun `isMain returns true only for main derivation index`() {
        // Arrange
        val main = DerivationIndex.Main
        val notMain = DerivationIndex(1).getOrNull()!!

        // Act & Assert
        Truth.assertThat(main.isMain).isTrue()
        Truth.assertThat(notMain.isMain).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun invoke(model: InvokeTestModel) {
        // Act
        val actual = DerivationIndex(model.index)

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        InvokeTestModel(index = 0, expected = DerivationIndex.Main.right()),
        InvokeTestModel(index = 5, expected = DerivationIndex(5).getOrNull()!!.right()),
        InvokeTestModel(index = -1, expected = DerivationIndex.Error.NegativeDerivationIndex(-1).left()),
    )

    data class InvokeTestModel(
        val index: Int,
        val expected: Either<DerivationIndex.Error, DerivationIndex>,
    )
}