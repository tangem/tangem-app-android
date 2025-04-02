package com.tangem.datasource.api.common.response

import com.tangem.core.analytics.api.AnalyticsErrorHandler
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ApiResponseCallAdapterFactory private constructor(
    private val analyticsErrorHandler: AnalyticsErrorHandler,
) : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) {
            return null
        }

        val callType = getParameterUpperBound(0, returnType as ParameterizedType)
        if (getRawType(callType) != ApiResponse::class.java) {
            return null
        }

        val resultType = getParameterUpperBound(0, callType as ParameterizedType)
        return ApiResponseCallAdapter(resultType, analyticsErrorHandler)
    }

    companion object {

        fun create(analyticsErrorHandler: AnalyticsErrorHandler) = ApiResponseCallAdapterFactory(analyticsErrorHandler)
    }
}