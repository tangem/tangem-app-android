package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.TokensGroupType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokensGroupTypeConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Act
            val actual = TokensGroupTypeConverter.convert(model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels(): List<ConvertModel> {
            return listOf(
                ConvertModel(
                    value = UserTokensResponse.GroupType.NETWORK,
                    expected = TokensGroupType.NETWORK,
                ),
                ConvertModel(
                    value = UserTokensResponse.GroupType.NONE,
                    expected = TokensGroupType.NONE,
                ),
                ConvertModel(
                    value = UserTokensResponse.GroupType.TOKEN,
                    expected = TokensGroupType.NONE,
                ),
            )
        }
    }

    data class ConvertModel(
        val value: UserTokensResponse.GroupType,
        val expected: TokensGroupType,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = TokensGroupTypeConverter.convertBack(model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels(): List<ConvertBackModel> {
            return listOf(
                ConvertBackModel(
                    value = TokensGroupType.NETWORK,
                    expected = UserTokensResponse.GroupType.NETWORK,
                ),
                ConvertBackModel(
                    value = TokensGroupType.NONE,
                    expected = UserTokensResponse.GroupType.NONE,
                ),
            )
        }
    }

    data class ConvertBackModel(
        val value: TokensGroupType,
        val expected: UserTokensResponse.GroupType,
    )
}