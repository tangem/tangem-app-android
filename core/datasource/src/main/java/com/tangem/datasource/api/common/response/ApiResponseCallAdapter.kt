package com.tangem.datasource.api.common.response

import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

internal class ApiResponseCallAdapter(
    private val resultType: Type,
) : CallAdapter<Type, Call<ApiResponse<Type>>> {

    override fun responseType(): Type = resultType

    override fun adapt(call: Call<Type>): Call<ApiResponse<Type>> {
        return ApiResponseCallDelegate(call)
    }
}