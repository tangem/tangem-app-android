package com.tangem.datasource.remote.response

import com.tangem.core.remote.response.ApiResponse

import com.tangem.core.analytics.api.AnalyticsErrorHandler
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

internal class ApiResponseCallAdapter(
    private val resultType: Type,
    private val analyticsErrorHandler: AnalyticsErrorHandler,
) : CallAdapter<Type, Call<ApiResponse<Type>>> {

    override fun responseType(): Type = resultType

    override fun adapt(call: Call<Type>): Call<ApiResponse<Type>> {
        return ApiResponseCallDelegate(call, analyticsErrorHandler, isBodyOptional = resultType == Unit::class.java)
    }
}