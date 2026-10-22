package com.tangem.data.transaction

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.transaction.api.TransactionIntentApi
import com.tangem.data.transaction.api.models.ConfirmIntentTransactionRequest
import com.tangem.data.transaction.convertes.SendIntentConverter
import com.tangem.data.transaction.convertes.SendIntentErrorConverter
import com.tangem.domain.transaction.SendIntentRepository
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

internal class DefaultSendIntentRepository(
    private val transactionIntentApi: TransactionIntentApi,
    private val sendIntentConverter: SendIntentConverter,
    private val sendIntentErrorConverter: SendIntentErrorConverter,
) : SendIntentRepository {

    override suspend fun getSendIntent(intentId: String): Either<Throwable, SendIntent> {
        val result: Either<Throwable, SendIntent> = when (val response = transactionIntentApi.getSendIntent(intentId)) {
            is ApiResponse.Success -> sendIntentConverter.convert(response.data)
            is ApiResponse.Error -> Either.Left(convertReadError(intentId = intentId, cause = response.cause))
        }

        return result.onLeft { error -> logFailure(error, message = "Failed to read send intent $intentId") }
    }

    override suspend fun completeSendIntent(intentId: String, txHash: String): Either<Throwable, Unit> {
        val response = transactionIntentApi.confirmIntentTransaction(
            intentId = intentId,
            request = ConfirmIntentTransactionRequest(txHash = txHash),
        )
        val result: Either<Throwable, Unit> = when (response) {
            is ApiResponse.Success -> Either.Right(Unit)
            is ApiResponse.Error -> Either.Left(convertError(intentId = intentId, cause = response.cause))
        }

        return result.onLeft { error -> logFailure(error, message = "Failed to complete send intent $intentId") }
    }

    /** Completing the intent retries on any server failure itself, so only reading tells HTTP 5xx apart. */
    override suspend fun reportSendIntentHash(intentId: String, txHash: String): Either<Throwable, Unit> {
        var result = completeSendIntent(intentId = intentId, txHash = txHash)

        for (pause in RETRY_PAUSES) {
            if (result.leftOrNull()?.mayPass() != true) break
            delay(pause)
            result = completeSendIntent(intentId = intentId, txHash = txHash)
        }

        return result
    }

    private fun Throwable.mayPass(): Boolean = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> true
        is ApiResponseError.HttpException -> isServerError() || code in RETRIABLE_CLIENT_ERRORS
        else -> false
    }

    private fun convertReadError(intentId: String, cause: ApiResponseError): Throwable {
        val error = convertError(intentId = intentId, cause = cause)

        return if (error is ApiResponseError.HttpException && error.isServerError()) {
            SendIntentError.ServerError(intentId = intentId, httpCode = error.code.numericCode)
        } else {
            error
        }
    }

    private fun convertError(intentId: String, cause: ApiResponseError): Throwable {
        return sendIntentErrorConverter.convert(SendIntentErrorConverter.Input(intentId = intentId, cause = cause))
    }

    private fun logFailure(error: Throwable, message: String) {
        when (error) {
            is SendIntentError.NotFound,
            is SendIntentError.AlreadyCompleted,
            -> TangemLogger.i(error.message)
            else -> TangemLogger.e(message, error)
        }
    }

    private companion object {
        // Three more attempts after the first, as the requirements ask
        val RETRY_PAUSES = listOf(2.seconds, 5.seconds, 15.seconds)
        val RETRIABLE_CLIENT_ERRORS = setOf(
            ApiResponseError.HttpException.Code.UNAUTHORIZED,
            ApiResponseError.HttpException.Code.REQUEST_TIMEOUT,
            ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
        )
    }
}