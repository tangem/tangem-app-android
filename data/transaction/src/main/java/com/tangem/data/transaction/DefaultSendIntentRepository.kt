package com.tangem.data.transaction

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.transaction.api.TransactionIntentApi
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
            is ApiResponse.Error -> {
                val input = SendIntentErrorConverter.Input(intentId = intentId, cause = response.cause)
                Either.Left(sendIntentErrorConverter.convert(input))
            }
        }

        return result.onLeft { error ->
            when (error) {
                is SendIntentError.NotFound,
                is SendIntentError.SessionRevoked,
                -> TangemLogger.i(error.message)
                else -> TangemLogger.e("Failed to read send intent $intentId", error)
            }
        }
    }
}