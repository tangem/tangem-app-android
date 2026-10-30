package com.tangem.data.transaction

import android.os.SystemClock
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.JsonAdapter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.grow.datasource.gasless.SolanaGaslessApi
import com.tangem.grow.datasource.gasless.models.GaslessErrorDto
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.tangem.grow.datasource.gasless.models.GaslessServiceResponse
import com.tangem.grow.datasource.gasless.models.solana.SolanaSubmitRequestBody
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Base64

internal class DefaultSolanaGaslessTransactionRepository(
    private val api: SolanaGaslessApi,
    private val errorAdapter: JsonAdapter<GaslessErrorResponse>,
    private val dispatchers: CoroutineDispatcherProvider,
    // Monotonic, so a wall clock set back cannot keep a cached fee payer (or failure) alive past its TTL
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : SolanaGaslessTransactionRepository {

    private val feePayerMutex = Mutex()
    private var cachedFeePayer: Either<SolanaGaslessError, String>? = null
    private var cachedFeePayerAtMillis: Long = 0L

    // Process-wide (singleton). The lookup doubles as the availability check, so a success expires too: otherwise a
    // service that turned Solana off keeps gasless offered until a submit fails. A failure is cached briefly
    // so the 10s quote refresh does not hit the network, and the log, on every tick. Submit errors below also clear it.
    override suspend fun getFeePayer(): Either<SolanaGaslessError, String> = feePayerMutex.withLock {
        val cached = cachedFeePayer
        if (cached != null && !isCacheExpired(cached)) return@withLock cached

        withContext(dispatchers.io) { api.getFeePayer().unwrap(isSubmit = false) { it.feePayer } }
            .also { fetched ->
                cachedFeePayer = fetched
                cachedFeePayerAtMillis = elapsedRealtimeMillis()
            }
    }

    override suspend fun submit(
        userWalletId: UserWalletId,
        transaction: ByteArray,
        lastValidBlockHeight: Long,
    ): Either<SolanaGaslessError, String> {
        val result = withContext(dispatchers.io) {
            val response = api.submit(
                userWalletId = userWalletId.stringValue,
                // The service rejects line-wrapped base64 (android.util.Base64.DEFAULT) as SOLANA_DECODE_FAILED
                body = SolanaSubmitRequestBody(Base64.getEncoder().encodeToString(transaction), lastValidBlockHeight),
            )
            response.alreadyBroadcastSignature()?.right() ?: response.unwrap(isSubmit = true) { it.signature }
        }
        result.onLeft { error ->
            // These can mean the sponsor key rotated or the service changed, so a stale feePayer must not be reused
            if (error.invalidatesFeePayerCache()) feePayerMutex.withLock { cachedFeePayer = null }
        }
        return result
    }

    /** A 409 for bytes the service already broadcast is the earlier submit's success, not a failure of this one. */
    private fun ApiResponse<*>.alreadyBroadcastSignature(): String? {
        val httpError = (this as? ApiResponse.Error)?.cause as? ApiResponseError.HttpException ?: return null
        if (httpError.code != ApiResponseError.HttpException.Code.CONFLICT) return null
        val error = httpError.parseError(errorAdapter) ?: return null
        val isBroadcast = error.code == DUPLICATE_SUBMISSION_CODE && error.details?.status == BROADCAST_STATUS
        return error.details?.signature?.takeIf { isBroadcast }
            ?.also { TangemLogger.i("Solana gasless submit: the service already broadcast $it") }
    }

    private fun SolanaGaslessError.invalidatesFeePayerCache(): Boolean = this is SolanaGaslessError.Unauthorized ||
        this is SolanaGaslessError.Disabled ||
        this is SolanaGaslessError.Rejected

    private fun isCacheExpired(cached: Either<SolanaGaslessError, String>): Boolean {
        val ttlMillis = if (cached.isRight()) FEE_PAYER_CACHE_MILLIS else FAILED_FEE_PAYER_CACHE_MILLIS
        return elapsedRealtimeMillis() - cachedFeePayerAtMillis >= ttlMillis
    }

    private inline fun <T : Any, R> ApiResponse<GaslessServiceResponse<T>>.unwrap(
        isSubmit: Boolean,
        transform: (T) -> R,
    ): Either<SolanaGaslessError, R> = when (this) {
        is ApiResponse.Success -> if (data.isSuccess) {
            transform(data.result).right()
        } else {
            SolanaGaslessError.Unknown("Unsuccessful gasless response").left()
        }
        is ApiResponse.Error -> cause.toSolanaGaslessError(isSubmit)
            .also { TangemLogger.e("Solana gasless request failed: $it") }
            .left()
    }

    private fun ApiResponseError.toSolanaGaslessError(isSubmit: Boolean): SolanaGaslessError = when (this) {
        is ApiResponseError.HttpException -> mapHttp(code, parseError(errorAdapter), isSubmit)
        // Only submit broadcasts bytes; a timeout, connection reset or malformed body there may still mean the tx
        // landed, so rebuilding risks a double spend. getFeePayer never broadcasts, so it keeps the plain mapping.
        is ApiResponseError.TimeoutException,
        is ApiResponseError.NetworkException,
        -> if (isSubmit) {
            SolanaGaslessError.MaybeSent(signature = null)
        } else {
            SolanaGaslessError.ServiceUnavailable(code = null)
        }
        is ApiResponseError.UnknownException ->
            if (isSubmit) SolanaGaslessError.MaybeSent(signature = null) else SolanaGaslessError.Unknown(cause.message)
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
        val RETRIABLE_CLIENT_ERRORS = setOf(
            ApiResponseError.HttpException.Code.REQUEST_TIMEOUT,
            ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
        )
        const val FEE_PAYER_CACHE_MILLIS = 600_000L
        const val FAILED_FEE_PAYER_CACHE_MILLIS = 60_000L
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