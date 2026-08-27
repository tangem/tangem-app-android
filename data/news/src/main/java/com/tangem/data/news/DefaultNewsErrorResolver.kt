package com.tangem.data.news

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.news.NewsError
import com.tangem.domain.news.NewsErrorResolver

internal class DefaultNewsErrorResolver : NewsErrorResolver {

    override fun resolve(throwable: Throwable?): NewsError {
        return when (throwable) {
            is ApiResponseError.HttpException -> {
                NewsError.HttpError(
                    code = throwable.code.numericCode,
                    message = throwable.message.orEmpty(),
                )
            }
            else -> {
                NewsError.NotHttpError
            }
        }
    }
}