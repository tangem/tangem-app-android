package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.domain.models.account.AccountName
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountNameConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Act
            val actual = AccountNameConverter.convert(value = model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels(): List<ConvertModel> {
            return listOf(
                ConvertModel(
                    value = AccountName.Custom("MyAccount").getOrNull()!!,
                    expected = "MyAccount",
                ),
                ConvertModel(value = AccountName.DefaultMain, expected = null),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = AccountNameConverter.convertBack(value = model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels(): List<ConvertBackModel> {
            return listOf(
                ConvertBackModel(value = "MyAccount", expected = AccountName.Custom("MyAccount").getOrNull()!!),
                ConvertBackModel(value = null, expected = AccountName.DefaultMain),
                // iOS limits the name to 20 characters the user sees; each of these emoji is 2 UTF-16 units
                ConvertBackModel(
                    value = EMOJI_NAME,
                    expected = AccountName.Custom.fromStored(EMOJI_NAME).getOrNull()!!,
                ),
            )
        }

        @Test
        fun `GIVEN a blank stored name WHEN convertBack THEN it is refused`() {
            // Act
            val actual = runCatching { AccountNameConverter.convertBack(value = "   ") }.exceptionOrNull()

            // Assert
            Truth.assertThat(actual).isInstanceOf(IllegalStateException::class.java)
        }
    }

    data class ConvertModel(val value: AccountName, val expected: String?)

    data class ConvertBackModel(val value: String?, val expected: AccountName)

    private companion object {
        /** 14 characters the user sees, 21 UTF-16 units */
        val EMOJI_NAME = "Family \uD83E\uDD79" + "\uD83E\uDD79".repeat(n = 6)
    }
}