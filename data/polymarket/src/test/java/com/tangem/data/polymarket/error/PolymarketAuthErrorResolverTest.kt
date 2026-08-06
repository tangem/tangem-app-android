package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.polymarket.model.PolymarketAuthError
import org.junit.jupiter.api.Test

internal class PolymarketAuthErrorResolverTest {

    private val resolver = PolymarketAuthErrorResolver()

    @Test
    fun `GIVEN 401 WHEN resolve THEN InvalidSignature`() {
        val error = ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.UNAUTHORIZED, message = "error", errorBody = null)
        assertThat(resolver.resolve(error)).isEqualTo(PolymarketAuthError.InvalidSignature)
    }

    @Test
    fun `GIVEN 404 WHEN resolve THEN KeyNotFound`() {
        val error = ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.NOT_FOUND, message = "error", errorBody = null)
        assertThat(resolver.resolve(error)).isEqualTo(PolymarketAuthError.KeyNotFound)
    }

    @Test
    fun `GIVEN 429 WHEN resolve THEN RateLimited`() {
        val error = ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS, message = "error", errorBody = null)
        assertThat(resolver.resolve(error)).isEqualTo(PolymarketAuthError.RateLimited)
    }

    @Test
    fun `GIVEN network exception WHEN resolve THEN Network`() {
        assertThat(resolver.resolve(ApiResponseError.NetworkException())).isEqualTo(PolymarketAuthError.Network)
    }
}