package com.tangem.data.transaction

import com.squareup.moshi.JsonAdapter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.grow.datasource.gasless.models.GaslessErrorDto
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.tangem.utils.logging.TangemLogger

/**
 * Maps the Solana gasless service's failures to [SolanaGaslessError].
 *
 * Submit is the only call that broadcasts, so it maps every answer that may come after a broadcast to
 * [SolanaGaslessError.MaybeSent]: rebuilding such a tx risks a double spend. getFeePayer keeps the plain mapping.
 */
internal class SolanaGaslessErrorMapper(private val errorAdapter: JsonAdapter<GaslessErrorResponse>) {

    /** A 409 for bytes the service already broadcast is the earlier submit's success, not a failure of this one. */
    fun alreadyBroadcastSignature(response: ApiResponse<*>): String? {
        val httpError = (response as? ApiResponse.Error)?.cause as? ApiResponseError.HttpException ?: return null
        if (httpError.code != ApiResponseError.HttpException.Code.CONFLICT) return null
        val error = httpError.parseError(errorAdapter) ?: return null
        val isBroadcast = error.code == DUPLICATE_SUBMISSION_CODE && error.details?.status == BROADCAST_STATUS
        return error.details?.signature?.takeIf { isBroadcast }
            ?.also { TangemLogger.i("Solana gasless submit: the service already broadcast $it") }
    }

    fun map(cause: ApiResponseError, isSubmit: Boolean): SolanaGaslessError = when (cause) {
        is ApiResponseError.HttpException -> mapHttp(cause.code, cause.parseError(errorAdapter), isSubmit)
        // A timeout, connection reset or malformed body after submit may still mean the tx landed
        is ApiResponseError.TimeoutException,
        is ApiResponseError.NetworkException,
        -> if (isSubmit) {
            SolanaGaslessError.MaybeSent(signature = null)
        } else {
            SolanaGaslessError.ServiceUnavailable(code = null)
        }
        is ApiResponseError.UnknownException -> if (isSubmit) {
            SolanaGaslessError.MaybeSent(signature = null)
        } else {
            SolanaGaslessError.Unknown(cause.cause.message)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapHttp(
        code: ApiResponseError.HttpException.Code,
        error: GaslessErrorDto?,
        isSubmit: Boolean,
    ): SolanaGaslessError {
        val errorCode = error?.code
        val signature = error?.details?.signature
        return when {
            code == ApiResponseError.HttpException.Code.UNAUTHORIZED ||
                code == ApiResponseError.HttpException.Code.FORBIDDEN -> SolanaGaslessError.Unauthorized
            errorCode == "UNSUPPORTED_CHAIN" -> SolanaGaslessError.Disabled
            errorCode == "SOLANA_BLOCKHASH_EXPIRED" -> SolanaGaslessError.BlockhashExpired
            errorCode == "SOLANA_RATE_LIMIT_EXCEEDED" || errorCode == "RATE_LIMIT_EXCEEDED" ->
                SolanaGaslessError.RateLimited(isPerWallet = error.details?.scope == "wallet")
            errorCode == "SOLANA_BROADCAST_UNCERTAIN" || errorCode == "EXTERNAL_SERVICE_TIMEOUT" ->
                SolanaGaslessError.MaybeSent(signature)
            // A 409 other than a finished broadcast (in flight, validated, failed, or one without the service's body)
            // may still land, so rebuilding risks a double spend
            errorCode == DUPLICATE_SUBMISSION_CODE ||
                isSubmit && code == ApiResponseError.HttpException.Code.CONFLICT ->
                SolanaGaslessError.MaybeSent(signature)
            code == ApiResponseError.HttpException.Code.UNPROCESSABLE_ENTITY ||
                code == ApiResponseError.HttpException.Code.PAYLOAD_TOO_LARGE -> SolanaGaslessError.Rejected(errorCode)
            // A gateway 5xx (or an unrecognised code) on submit may come while the backend is still broadcasting,
            // so only the codes known to fail before broadcast are safe to retry
            isSubmit && code.numericCode >= HTTP_SERVER_ERROR && errorCode !in NOT_BROADCAST_SERVER_ERROR_CODES ->
                SolanaGaslessError.MaybeSent(signature)
            code.numericCode >= HTTP_SERVER_ERROR -> SolanaGaslessError.ServiceUnavailable(
                code = errorCode,
                isSponsorshipUnavailable = errorCode in SPONSORSHIP_UNAVAILABLE_CODES,
            )
            // Any other 4xx on submit is the service judging this tx, so sending it again fails the same way
            isSubmit && code.numericCode in HTTP_CLIENT_ERRORS && code !in RETRIABLE_CLIENT_ERRORS ->
                SolanaGaslessError.Rejected(errorCode)
            else -> SolanaGaslessError.Unknown(error?.message)
        }
    }

    private companion object {
        const val HTTP_SERVER_ERROR = 500
        val HTTP_CLIENT_ERRORS = 400 until HTTP_SERVER_ERROR
        // Each asks to retry later or to rebuild (a bare 410), so they do not refuse the tx
        val RETRIABLE_CLIENT_ERRORS = setOf(
            ApiResponseError.HttpException.Code.REQUEST_TIMEOUT,
            ApiResponseError.HttpException.Code.GONE,
            ApiResponseError.HttpException.Code.LOCKED,
            ApiResponseError.HttpException.Code.TOO_EARLY,
            ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
        )
        const val DUPLICATE_SUBMISSION_CODE = "SOLANA_DUPLICATE_SUBMISSION"
        const val BROADCAST_STATUS = "broadcast"
        // Each says the request sent nothing; the app never resubmits the same bytes, so that covers the transaction
        val NOT_BROADCAST_SERVER_ERROR_CODES = setOf(
            "SOLANA_SPEND_CEILING_EXCEEDED",
            "SPONSOR_INSUFFICIENT_FUNDS",
            "SOLANA_SPONSOR_INSUFFICIENT_FUNDS",
            "BROADCAST_FAILED",
            "SOLANA_BROADCAST_FAILED",
            "CONFIGURATION_MISSING",
            "EXTERNAL_SERVICE_ERROR",
        )
        val SPONSORSHIP_UNAVAILABLE_CODES = setOf(
            "SOLANA_SPEND_CEILING_EXCEEDED",
            "SPONSOR_INSUFFICIENT_FUNDS",
            "SOLANA_SPONSOR_INSUFFICIENT_FUNDS",
            "CONFIGURATION_MISSING",
        )
    }
}