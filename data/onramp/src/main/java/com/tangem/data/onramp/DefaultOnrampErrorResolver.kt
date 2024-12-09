package com.tangem.data.onramp

import com.tangem.data.onramp.converters.error.OnrampErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.error.OnrampRedirectError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver

internal class DefaultOnrampErrorResolver(
    private val onrampErrorConverter: OnrampErrorConverter,
) : OnrampErrorResolver {

    override fun resolve(throwable: Throwable): OnrampError {
        return when (throwable) {
            is ApiResponseError.HttpException -> {
                onrampErrorConverter.convert(throwable.errorBody.orEmpty())
            }
            is OnrampRedirectError.WrongRequestId -> OnrampError.RedirectError.WrongRequestId
            is OnrampRedirectError.VerificationFailed -> OnrampError.RedirectError.VerificationFailed
            else -> {
                OnrampError.DomainError(throwable.message)
            }
        }
    }
}
