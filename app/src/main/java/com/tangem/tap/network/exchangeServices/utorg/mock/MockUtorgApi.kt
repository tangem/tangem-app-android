package com.tangem.tap.network.exchangeServices.utorg.mock

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.network.exchangeServices.utorg.api.RequestSuccessUrl
import com.tangem.tap.network.exchangeServices.utorg.api.UtorgApi
import com.tangem.tap.network.exchangeServices.utorg.api.UtorgErrorResponse
import com.tangem.tap.network.exchangeServices.utorg.api.UtorgErrorType
import com.tangem.tap.network.exchangeServices.utorg.api.model.UtorgCurrencyResponse
import kotlinx.coroutines.delay

/**
* [REDACTED_AUTHOR]
 */
class MockUtorgApi : UtorgApi {

    var nextDelay = 500L

    var nextResponseType = ResponseType.Success
    var nextErrorType = UtorgErrorType.UNKNOWN_ERROR

    override suspend fun getCurrency(apiVersion: String): UtorgCurrencyResponse {
        delay(nextDelay)
        return UtorgCurrencyResponse(
            success = createIsSuccess(),
            timestamp = createTime(),
            data = createData(MockUtorgSuccessDataResponse.GetCurrency),
            error = createError(),
        )
    }

    override suspend fun setSuccessUrl(apiVersion: String, request: RequestSuccessUrl) {
        delay(nextDelay)
    }

    private fun createIsSuccess(): Boolean = when (nextResponseType) {
        ResponseType.Success -> true
        ResponseType.Error -> false
    }

    private fun createTime(): Long = System.currentTimeMillis()

    private inline fun <reified T> createData(response: MockUtorgSuccessDataResponse): T? = when (nextResponseType) {
        ResponseType.Success -> MoshiJsonConverter.INSTANCE.fromJson(response.json)!!
        ResponseType.Error -> null
    }

    private fun createError(): UtorgErrorResponse? = when (nextResponseType) {
        ResponseType.Success -> null
        ResponseType.Error -> UtorgErrorResponse(
            message = "Optional message",
            type = nextErrorType,
        )
    }

    enum class ResponseType {
        Success, Error
    }
}
