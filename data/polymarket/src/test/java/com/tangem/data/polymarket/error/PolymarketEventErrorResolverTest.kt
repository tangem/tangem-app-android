package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketEventErrorResolverTest {

    private val resolver = PolymarketEventErrorResolver()

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) {
        // Act
        val actual = resolver.resolve(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN an unknown exception WHEN resolve THEN its message is carried`() {
        // Arrange
        val error = ApiResponseError.UnknownException(cause = IllegalStateException("boom"))

        // Act
        val actual = resolver.resolve(error)

        // Assert
        assertThat(actual).isEqualTo(PolymarketEventError.Unknown(httpCode = null, detail = "boom"))
    }

    internal data class ResolveModel(val error: ApiResponseError, val expected: PolymarketEventError)

    private fun provideTestModels(): List<ResolveModel> = listOf(
        // An event the BFF no longer serves is the one dead end worth telling apart.
        ResolveModel(error = httpError(Code.NOT_FOUND), expected = PolymarketEventError.NotFound),
        ResolveModel(
            error = httpError(Code.INTERNAL_SERVER_ERROR),
            expected = PolymarketEventError.Unknown(httpCode = 500, detail = "body"),
        ),
        ResolveModel(
            error = httpError(Code.BAD_REQUEST),
            expected = PolymarketEventError.Unknown(httpCode = 400, detail = "body"),
        ),
        ResolveModel(error = ApiResponseError.NetworkException(), expected = PolymarketEventError.Network),
        ResolveModel(error = ApiResponseError.TimeoutException(), expected = PolymarketEventError.Network),
    )

    private fun httpError(code: Code) = ApiResponseError.HttpException(
        code = code,
        message = "error",
        errorBody = "body",
    )
}