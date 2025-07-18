package com.tangem.data.swap

import com.tangem.data.express.converter.ExpressErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.swap.SwapErrorResolver

internal class DefaultSwapErrorResolver(
    private val expressErrorConverter: ExpressErrorConverter,
) : SwapErrorResolver {
    override fun resolve(throwable: Throwable): ExpressError {
        return when (throwable) {
            is ApiResponseError.HttpException -> {
                expressErrorConverter.convert(throwable.errorBody.orEmpty())
            }
            is ExpressError -> throwable
            else -> ExpressError.UnknownError
        }
    }
}