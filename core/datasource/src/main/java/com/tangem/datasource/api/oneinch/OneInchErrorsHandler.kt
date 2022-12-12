package com.tangem.datasource.api.oneinch

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.oneinch.models.SwapErrorDto
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class OneInchErrorsHandler @Inject constructor(private val moshi: Moshi) {

    fun <T> handleOneInchResponse(response: Response<T>): BaseOneInchResponse<T> {
        val body = response.body()
        return if (response.isSuccessful) {
            return BaseOneInchResponse(body, null)
        } else {
            when (response.code()) {
                HTTP_CODE_400 -> {
                    if (body != null) {
                        BaseOneInchResponse(null, moshi.adapter(SwapErrorDto::class.java).fromJson(body.toString()))
                    } else {
                        throw HttpException(response)
                    }
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