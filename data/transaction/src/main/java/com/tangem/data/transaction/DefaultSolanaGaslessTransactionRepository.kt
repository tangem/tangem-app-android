package com.tangem.data.transaction

import android.os.SystemClock
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.JsonAdapter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.grow.datasource.gasless.SolanaGaslessApi
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
    errorAdapter: JsonAdapter<GaslessErrorResponse>,
    private val dispatchers: CoroutineDispatcherProvider,
    // Monotonic, so a wall clock set back cannot keep a cached fee payer (or failure) alive past its TTL
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : SolanaGaslessTransactionRepository {

    private val errorMapper = SolanaGaslessErrorMapper(errorAdapter)
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
            errorMapper.alreadyBroadcastSignature(response)?.right()
                ?: response.unwrap(isSubmit = true) { it.signature }
        }
        result.onLeft { error ->
            // These can mean the sponsor key rotated or the service changed, so a stale feePayer must not be reused
            if (error.invalidatesFeePayerCache()) feePayerMutex.withLock { cachedFeePayer = null }
        }
        return result
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
        is ApiResponse.Error -> errorMapper.map(cause, isSubmit)
            .also { TangemLogger.e("Solana gasless request failed: $it") }
            .left()
    }

    private companion object {
        const val FEE_PAYER_CACHE_MILLIS = 600_000L
        const val FAILED_FEE_PAYER_CACHE_MILLIS = 60_000L
    }
}