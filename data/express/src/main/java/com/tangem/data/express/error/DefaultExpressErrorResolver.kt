package com.tangem.data.express.error

import com.tangem.data.express.converter.ExpressErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.express.ExpressErrorResolver
import com.tangem.domain.express.models.ExpressError

internal class DefaultExpressErrorResolver(
    private val expressErrorConverter: ExpressErrorConverter,
) : ExpressErrorResolver {

    override fun resolve(throwable: Throwable): ExpressError {
        return when (throwable) {
            is ApiResponseError.HttpException -> {
                expressErrorConverter.convert(throwable.errorBody.orEmpty())
            }
            else -> ExpressError.UnknownError
        }
    }
}