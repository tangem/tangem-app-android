package com.tangem.datasource.api.oneinch

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.oneinch.errors.OneIncResponseException
import com.tangem.datasource.api.oneinch.models.SwapErrorDto
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class OneInchErrorsHandler @Inject constructor(moshi: Moshi) {

    private val errorMoshiAdapter = moshi.adapter(SwapErrorDto::class.java)

    @Throws(OneIncResponseException::class)
    fun <T> handleOneInchResponse(response: Response<T>): T {
        return if (response.isSuccessful) {
            response.body() ?: error("response body is null")
        } else {
            when (response.code()) {
                HTTP_CODE_400 -> {
                    val swapErrorDto = errorMoshiAdapter.fromJson(response.errorBody()?.string() ?: "")
                    throw OneIncResponseException(swapErrorDto ?: throw HttpException(response))
                }
                else -> {
                    throw HttpException(response)
                }
            }
        }
    }

    companion object {
        private const val HTTP_CODE_400 = 400
    }
}
