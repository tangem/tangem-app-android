package com.tangem.data.earn

import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.models.earn.EarnError

internal class DefaultEarnErrorResolver : EarnErrorResolver {

    override fun resolve(throwable: Throwable?): EarnError {
        return when (throwable) {
            is ApiResponseError.HttpException -> {
                EarnError.HttpError(
                    code = throwable.code.numericCode,
                    message = throwable.message.orEmpty(),
                )
            }
            else -> EarnError.NotHttpError()
        }
    }
}