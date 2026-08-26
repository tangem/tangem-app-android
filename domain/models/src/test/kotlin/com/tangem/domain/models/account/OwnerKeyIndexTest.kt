package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnerKeyIndexTest {

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: InvokeTestModel) {
        // Act
        val actual = OwnerKeyIndex(model.index)

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        InvokeTestModel(index = 0, expected = OwnerKeyIndex(0).getOrNull()!!.right()),
        InvokeTestModel(index = 5, expected = OwnerKeyIndex(5).getOrNull()!!.right()),
        InvokeTestModel(index = -1, expected = OwnerKeyIndex.Error.NegativeOwnerKeyIndex(-1).left()),
    )

    data class InvokeTestModel(
        val index: Int,
        val expected: Either<OwnerKeyIndex.Error, OwnerKeyIndex>,
    )
}