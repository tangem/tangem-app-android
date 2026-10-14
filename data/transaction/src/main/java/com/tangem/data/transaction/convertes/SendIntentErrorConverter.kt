package com.tangem.data.transaction.convertes

import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.transaction.api.models.TransactionIntentErrorResponse
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 * Turns a failed intent request into a [SendIntentError] when the service names a known reason, and keeps the raw
 * failure otherwise.
 *
 * The reason is read from the `code` of the error body, not the HTTP status: the contract gives a revoked session
 * both 409 and 410.
 */
internal class SendIntentErrorConverter @Inject constructor() :
    Converter<SendIntentErrorConverter.Input, Throwable> {

    private val json = NetworkJsonFactory.create()

    override fun convert(value: Input): Throwable {
        val code = (value.cause as? ApiResponseError.HttpException)?.readCode()

        return when (code) {
            CODE_NOT_FOUND -> SendIntentError.NotFound(intentId = value.intentId)
            CODE_SESSION_REVOKED -> SendIntentError.SessionRevoked(intentId = value.intentId)
            else -> value.cause
        }
    }

    private fun ApiResponseError.HttpException.readCode(): String? {
        val body = errorBody?.takeIf(String::isNotBlank) ?: return null
        // A proxy may answer with an HTML page instead of the service's problem document: then there is no code
        return runCatching { json.decodeFromString<TransactionIntentErrorResponse>(body).code }.getOrNull()
    }

    data class Input(val intentId: String, val cause: ApiResponseError)

    private companion object {
        const val CODE_NOT_FOUND = "INTENT_NOT_FOUND"
        const val CODE_SESSION_REVOKED = "INTENT_SESSION_REVOKED"
    }
}