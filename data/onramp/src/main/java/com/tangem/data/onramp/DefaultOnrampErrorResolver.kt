package com.tangem.data.onramp

import com.tangem.data.onramp.converters.error.OnrampErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver

internal class DefaultOnrampErrorResolver(
    private val onrampErrorConverter: OnrampErrorConverter,
) : OnrampErrorResolver {

    override fun resolve(throwable: Throwable): OnrampError {
        return if (throwable is ApiResponseError.HttpException) {
            onrampErrorConverter.convert(throwable.errorBody.orEmpty())
        } else {
            OnrampError.UnknownError
        }
    }
}
