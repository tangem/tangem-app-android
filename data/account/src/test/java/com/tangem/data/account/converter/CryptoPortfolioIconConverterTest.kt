package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.account.converter.CryptoPortfolioIconConverter.DataModel
import com.tangem.domain.models.account.CryptoPortfolioIcon
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CryptoPortfolioIconConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: TestModel) {
        // Act
        val actual = runCatching { CryptoPortfolioIconConverter.convert(model.value) }

        // Assert
        actual
            .onSuccess {
                val expected = model.expected.getOrNull()!!
                Truth.assertThat(it).isEqualTo(expected)
            }
            .onFailure {
                val expected = model.expected.exceptionOrNull()!!
                Truth.assertThat(it).isInstanceOf(expected::class.java)
                Truth.assertThat(it.message).isEqualTo(expected.message)
            }
    }

    private fun provideTestModels(): List<TestModel> {
        return listOf(
            TestModel(
                value = DataModel(icon = "Letter", color = "Azure"),
                expected = Result.success(
                    CryptoPortfolioIcon.ofCustomAccount(
                        value = CryptoPortfolioIcon.Icon.Letter,
                        color = CryptoPortfolioIcon.Color.Azure,
                    ),
                ),
            ),
            TestModel(
                value = DataModel(icon = "INVALID_ICON", color = "Azure"),
                expected = Result.failure(
                    IllegalArgumentException(
                        "No enum constant com.tangem.domain.models.account.CryptoPortfolioIcon.Icon.INVALID_ICON",
                    ),
                ),
            ),
            TestModel(
                value = DataModel(icon = "Letter", color = "INVALID_COLOR"),
                expected = Result.failure(
                    IllegalArgumentException(
                        "No enum constant com.tangem.domain.models.account.CryptoPortfolioIcon.Color.INVALID_COLOR",
                    ),
                ),
            ),
        )
    }

    data class TestModel(
        val value: DataModel,
        val expected: Result<CryptoPortfolioIcon>,
    )
}