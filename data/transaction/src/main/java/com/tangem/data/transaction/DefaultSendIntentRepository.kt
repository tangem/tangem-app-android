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

internal class DefaultSendIntentRepository(
    private val transactionIntentApi: TransactionIntentApi,
    private val sendIntentConverter: SendIntentConverter,
    private val sendIntentErrorConverter: SendIntentErrorConverter,
) : SendIntentRepository {

    override suspend fun getSendIntent(intentId: String): Either<Throwable, SendIntent> {
        val result: Either<Throwable, SendIntent> = when (val response = transactionIntentApi.getSendIntent(intentId)) {
            is ApiResponse.Success -> sendIntentConverter.convert(response.data)
            is ApiResponse.Error -> Either.Left(convertError(intentId = intentId, cause = response.cause))
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

    private fun convertError(intentId: String, cause: ApiResponseError): Throwable {
        return sendIntentErrorConverter.convert(SendIntentErrorConverter.Input(intentId = intentId, cause = cause))
    }

    private fun logFailure(error: Throwable, message: String) {
        when (error) {
            is SendIntentError.NotFound,
            is SendIntentError.SessionRevoked,
            is SendIntentError.AlreadyCompleted,
            -> TangemLogger.i(error.message)
            else -> TangemLogger.e(message, error)
        }
    }
}