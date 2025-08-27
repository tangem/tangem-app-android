package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.TokensSortType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokensSortTypeConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Act
            val actual = TokensSortTypeConverter.convert(model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        fun provideTestModels() = listOf(
            ConvertModel(
                value = UserTokensResponse.SortType.BALANCE,
                expected = TokensSortType.BALANCE,
            ),
            ConvertModel(
                value = UserTokensResponse.SortType.MANUAL,
                expected = TokensSortType.NONE,
            ),
            ConvertModel(
                value = UserTokensResponse.SortType.MARKETCAP,
                expected = TokensSortType.NONE,
            ),
        )
    }

    data class ConvertModel(
        val value: UserTokensResponse.SortType,
        val expected: TokensSortType,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = TokensSortTypeConverter.convertBack(model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        fun provideTestModels() = listOf(
            ConvertBackModel(
                value = TokensSortType.BALANCE,
                expected = UserTokensResponse.SortType.BALANCE,
            ),
            ConvertBackModel(
                value = TokensSortType.NONE,
                expected = UserTokensResponse.SortType.MANUAL,
            ),
        )
    }

    data class ConvertBackModel(
        val value: TokensSortType,
        val expected: UserTokensResponse.SortType,
    )
}