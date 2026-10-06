package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountNameTest {

    @ParameterizedTest
    @ProvideTestModels
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

    @Test
    fun `GIVEN a stored name longer than the limit in UTF-16 WHEN fromStored THEN it is kept as is`() {
        // Arrange
        val storedName = " Family \uD83E\uDD79${"\uD83E\uDD79".repeat(n = 6)} "

        // Act
        val actual = AccountName.Custom.fromStored(value = storedName)

        // Assert
        Truth.assertThat(actual.getOrNull()?.value).isEqualTo(storedName.trim())
    }

    @Test
    fun `GIVEN a blank stored name WHEN fromStored THEN it is refused`() {
        // Act
        val actual = AccountName.Custom.fromStored(value = "  ")

        // Assert
        Truth.assertThat(actual).isEqualTo(AccountName.Error.Empty.left())
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