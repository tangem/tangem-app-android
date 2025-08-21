package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountNameTest {

    @Test
    fun main_returnsMainAccountName() {
        // Act
        val main = AccountName.Main.value

        // Assert
        val expected = "Main Account"
        Truth.assertThat(main).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun invoke(model: InvokeTestModel) {
        // Act
        val actual = AccountName(value = model.value)

        // Assert
        actual
            .onRight {
                val expected = model.expected.getOrNull()!!
                Truth.assertThat(it).isEqualTo(expected)
            }
            .onLeft {
                val expected = model.expected.leftOrNull()!!
                Truth.assertThat(it).isEqualTo(expected)
            }
    }

    private fun provideTestModels() = listOf(
        InvokeTestModel(
            value = "",
            expected = AccountName.Error.Empty.left(),
        ),
        InvokeTestModel(
            value = " ",
            expected = AccountName.Error.Empty.left(),
        ),
        InvokeTestModel(
            value = "a".repeat(21),
            expected = AccountName.Error.ExceedsMaxLength.left(),
        ),
        "a".repeat(20).let { value ->
            InvokeTestModel(
                value = value,
                expected = AccountName(value = value),
            )
        },
        InvokeTestModel(
            value = " name ",
            expected = AccountName(value = "name"),
        ),
        InvokeTestModel(
            value = "Main Account",
            expected = AccountName(value = "Main Account"),
        ),
    )

    data class InvokeTestModel(
        val value: String,
        val expected: Either<AccountName.Error, AccountName>,
    )
}