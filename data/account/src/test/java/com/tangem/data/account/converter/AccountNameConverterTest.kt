package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.domain.models.account.AccountName
import org.junit.jupiter.api.Nested
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
            )
        }
    }

    data class ConvertModel(val value: AccountName, val expected: String?)

    data class ConvertBackModel(val value: String?, val expected: AccountName)
}