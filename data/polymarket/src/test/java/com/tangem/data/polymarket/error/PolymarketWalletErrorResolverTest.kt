package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletError.RelayerRejected
import org.junit.jupiter.api.Test

internal class PolymarketWalletErrorResolverTest {

    private val resolver = PolymarketWalletErrorResolver(Moshi.Builder().build())

    @Test
    fun `GIVEN 400 WHEN resolve THEN InvalidRequest`() {
        assertThat(resolver.resolve(http(Code.BAD_REQUEST, problem("bad params"))))
            .isEqualTo(PolymarketWalletError.InvalidRequest)
    }

    @Test
    fun `GIVEN 401 WHEN resolve THEN Unauthorized`() {
        assertThat(resolver.resolve(http(Code.UNAUTHORIZED, body = null)))
            .isEqualTo(PolymarketWalletError.Unauthorized)
    }

    @Test
    fun `GIVEN 409 WHEN resolve THEN WalletNotDeployed`() {
        assertThat(resolver.resolve(http(Code.CONFLICT, problem("wallet is not deployed"))))
            .isEqualTo(PolymarketWalletError.WalletNotDeployed)
    }

    @Test
    fun `GIVEN 502 WHEN resolve THEN RelayerUnavailable`() {
        assertThat(resolver.resolve(http(Code.BAD_GATEWAY, body = null)))
            .isEqualTo(PolymarketWalletError.RelayerUnavailable)
    }

    @Test
    fun `GIVEN unmapped status WHEN resolve THEN Unexpected with code and detail`() {
        assertThat(resolver.resolve(http(Code.IM_A_TEAPOT, problem("i am a teapot"))))
            .isEqualTo(PolymarketWalletError.Unexpected(httpCode = 418, detail = "i am a teapot"))
    }

    @Test
    fun `GIVEN network exception WHEN resolve THEN Network`() {
        assertThat(resolver.resolve(ApiResponseError.NetworkException()))
            .isEqualTo(PolymarketWalletError.Network)
    }

    @Test
    fun `GIVEN timeout exception WHEN resolve THEN Network`() {
        assertThat(resolver.resolve(ApiResponseError.TimeoutException()))
            .isEqualTo(PolymarketWalletError.Network)
    }

    @Test
    fun `GIVEN UnknownException WHEN resolve THEN Unexpected with the cause message`() {
        // UnknownException's own message is null; the diagnostics must come from its cause.
        val error = ApiResponseError.UnknownException(cause = IllegalStateException("boom cause"))
        assertThat(resolver.resolve(error))
            .isEqualTo(PolymarketWalletError.Unexpected(httpCode = null, detail = "boom cause"))
    }

    @Test
    fun `GIVEN 422 relayer messages WHEN resolve THEN typed RelayerRejected variants`() {
        // Arrange: BFF retry-table detail strings → expected typed variant
        val cases = mapOf(
            "relayer rejected batch: wallet 0x1 is not registered" to RelayerRejected.WalletNotRegistered,
            "relayer rejected batch: deadline is too soon" to RelayerRejected.DeadlineTooSoon,
            "invalid signature" to RelayerRejected.InvalidSignature,
            "nonce reused" to RelayerRejected.NonceReused,
        )

        // Act + Assert
        cases.forEach { (detail, expected) ->
            assertThat(resolver.resolve(http(Code.UNPROCESSABLE_ENTITY, problem(detail))))
                .isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN 422 unknown message WHEN resolve THEN RelayerRejected Other with detail`() {
        assertThat(resolver.resolve(http(Code.UNPROCESSABLE_ENTITY, problem("some brand new reason"))))
            .isEqualTo(RelayerRejected.Other(detail = "some brand new reason"))
    }

    @Test
    fun `GIVEN 422 with unrelated nonce mention WHEN resolve THEN Other not NonceReused`() {
        // "nonce" alone must NOT be classified as NonceReused — only the specific "nonce reused" message is.
        assertThat(resolver.resolve(http(Code.UNPROCESSABLE_ENTITY, problem("invalid nonce format"))))
            .isEqualTo(RelayerRejected.Other(detail = "invalid nonce format"))
    }

    @Test
    fun `GIVEN 422 message in upper case WHEN resolve THEN matched case-insensitively`() {
        // Matching is case-insensitive and locale-invariant regardless of the detail's casing.
        assertThat(resolver.resolve(http(Code.UNPROCESSABLE_ENTITY, problem("INVALID SIGNATURE"))))
            .isEqualTo(RelayerRejected.InvalidSignature)
    }

    private fun http(code: Code, body: String?): ApiResponseError.HttpException =
        ApiResponseError.HttpException(code = code, message = "error", errorBody = body)

    private fun problem(detail: String): String =
        """{"type":"about:blank","title":"t","status":0,"detail":"$detail","instance":"/x"}"""
}