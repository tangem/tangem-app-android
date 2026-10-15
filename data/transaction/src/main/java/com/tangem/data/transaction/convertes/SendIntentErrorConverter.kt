package com.tangem.data.transaction.convertes

import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.transaction.api.models.TransactionIntentErrorCode
import com.tangem.data.transaction.api.models.TransactionIntentErrorResponse
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 * Turns a failed intent request into a [SendIntentError] when the service names a known reason, and keeps the raw
 * failure otherwise.
 *
 * The reason is read from the `code` of the error body, not the HTTP status, which a proxy or the gateway may answer
 * with as well.
 */
internal class SendIntentErrorConverter @Inject constructor() :
    Converter<SendIntentErrorConverter.Input, Throwable> {

    private val json = NetworkJsonFactory.create()

    override fun convert(value: Input): Throwable {
        val code = (value.cause as? ApiResponseError.HttpException)?.readCode()

        return when (code) {
            TransactionIntentErrorCode.INTENT_NOT_FOUND -> SendIntentError.NotFound(intentId = value.intentId)
            TransactionIntentErrorCode.INTENT_SESSION_REVOKED -> SendIntentError.SessionRevoked(value.intentId)
            TransactionIntentErrorCode.INTENT_ALREADY_COMPLETED -> SendIntentError.AlreadyCompleted(value.intentId)
            TransactionIntentErrorCode.UNKNOWN,
            null,
            -> value.cause
        }
    }

    private fun ApiResponseError.HttpException.readCode(): TransactionIntentErrorCode? {
        val body = errorBody?.takeIf(String::isNotBlank) ?: return null
        // A proxy may answer with an HTML page instead of the service's problem document: then there is no code
        return runCatching { json.decodeFromString<TransactionIntentErrorResponse>(body).code }.getOrNull()
    }

    data class Input(val intentId: String, val cause: ApiResponseError)
}