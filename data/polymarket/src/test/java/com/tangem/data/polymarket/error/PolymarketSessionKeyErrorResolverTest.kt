package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionKeyErrorResolverTest {

    private val resolver = PolymarketSessionKeyErrorResolver(ProblemDetailParser(Moshi.Builder().build()))

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a BFF failure WHEN resolve THEN maps it by reason first`(model: ResolveModel) {
        // Act
        val actual = resolver.resolve(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        reason(Code.BAD_REQUEST, "INVALID_WALLET_ID", PolymarketSessionKeyError.InvalidWalletId),
        reason(Code.BAD_REQUEST, "MALFORMED_REQUEST", PolymarketSessionKeyError.MalformedRequest),
        reason(Code.BAD_REQUEST, "INVALID_DEADLINE", PolymarketSessionKeyError.InvalidDeadline),
        reason(Code.BAD_REQUEST, "INVALID_VALID_UNTIL", PolymarketSessionKeyError.InvalidValidUntil),
        reason(Code.BAD_REQUEST, "INVALID_CALL", PolymarketSessionKeyError.InvalidCall),
        reason(Code.BAD_REQUEST, "INVALID_SIGNATURE", PolymarketSessionKeyError.InvalidSignature),
        reason(Code.FORBIDDEN, "WALLET_NOT_OWNED", PolymarketSessionKeyError.WalletNotOwned),
        reason(Code.NOT_FOUND, "SESSION_KEY_NOT_FOUND", PolymarketSessionKeyError.SessionKeyNotFound),
        reason(Code.CONFLICT, "WALLET_NOT_READY", PolymarketSessionKeyError.WalletNotReady),
        reason(
            Code.CONFLICT,
            "SESSION_KEY_OWNED_BY_ANOTHER_WALLET",
            PolymarketSessionKeyError.SessionKeyOwnedByAnotherWallet,
        ),
        reason(Code.CONFLICT, "SESSION_KEY_BUSY", PolymarketSessionKeyError.SessionKeyBusy),
        reason(Code.CONFLICT, "DEVICE_HAS_SESSION_KEY", PolymarketSessionKeyError.DeviceHasSessionKey),
        reason(Code.CONFLICT, "INVALID_REPLACEMENT", PolymarketSessionKeyError.InvalidReplacement),
        reason(Code.CONFLICT, "SESSION_KEY_LIMIT_REACHED", PolymarketSessionKeyError.SessionKeyLimitReached),
        reason(Code.CONFLICT, "SESSION_KEY_NOT_TOGGLEABLE", PolymarketSessionKeyError.SessionKeyNotToggleable),
        reason(
            Code.UNPROCESSABLE_ENTITY,
            "RELAYER_REJECTED",
            PolymarketSessionKeyError.RelayerRejected(detail = DETAIL),
        ),
        reason(Code.UNPROCESSABLE_ENTITY, "NONCE_ALREADY_USED", PolymarketSessionKeyError.NonceAlreadyUsed),
        reason(Code.BAD_GATEWAY, "UPSTREAM_UNAVAILABLE", PolymarketSessionKeyError.UpstreamUnavailable),
        ResolveModel(
            error = http(Code.UNAUTHORIZED, body = null),
            expected = PolymarketSessionKeyError.Unauthorized,
        ),
        ResolveModel(
            error = http(Code.BAD_GATEWAY, body = "<html>bad gateway</html>"),
            expected = PolymarketSessionKeyError.UpstreamUnavailable,
        ),
        ResolveModel(
            error = http(Code.CONFLICT, body = problem(reason = "SOMETHING_NEW")),
            expected = PolymarketSessionKeyError.Unknown(httpCode = 409, reason = "SOMETHING_NEW", detail = DETAIL),
        ),
        ResolveModel(
            error = http(Code.INTERNAL_SERVER_ERROR, body = null),
            expected = PolymarketSessionKeyError.Unknown(httpCode = 500, reason = null, detail = null),
        ),
        ResolveModel(error = ApiResponseError.NetworkException(), expected = PolymarketSessionKeyError.Network),
        ResolveModel(error = ApiResponseError.TimeoutException(), expected = PolymarketSessionKeyError.Network),
        ResolveModel(
            error = ApiResponseError.UnknownException(cause = IllegalStateException("boom")),
            expected = PolymarketSessionKeyError.Unknown(httpCode = null, reason = null, detail = "boom"),
        ),
    )

    private fun reason(code: Code, reason: String, expected: PolymarketSessionKeyError) =
        ResolveModel(error = http(code, problem(reason)), expected = expected)

    private fun http(code: Code, body: String?) =
        ApiResponseError.HttpException(code = code, message = "error", errorBody = body)

    private fun problem(reason: String) =
        """{"type":"about:blank","title":"t","status":0,"detail":"$DETAIL","reason":"$reason","instance":"/x"}"""

    data class ResolveModel(val error: ApiResponseError, val expected: PolymarketSessionKeyError)

    private companion object {
        const val DETAIL = "relayer said no"
    }
}