package com.tangem.data.onramp

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.onramp.converters.error.OnrampErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.error.OnrampRedirectError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver

internal class DefaultOnrampErrorResolver(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val onrampErrorConverter: OnrampErrorConverter,
) : OnrampErrorResolver {

    override fun resolve(throwable: Throwable): OnrampError {
        val error = when (throwable) {
            is ApiResponseError.HttpException -> {
                onrampErrorConverter.convert(throwable.errorBody.orEmpty())
            }
            is OnrampRedirectError.WrongRequestId -> OnrampError.RedirectError.WrongRequestId
            is OnrampRedirectError.VerificationFailed -> OnrampError.RedirectError.VerificationFailed
            else -> {
                OnrampError.DomainError(throwable.message)
            }
        }

        when (error) {
            is OnrampError.AmountError.TooBigError -> analyticsEventHandler.send(OnrampAnalyticsEvent.MaxAmountError)
            is OnrampError.AmountError.TooSmallError -> analyticsEventHandler.send(OnrampAnalyticsEvent.MinAmountError)
            is OnrampError.DataError -> analyticsEventHandler.send(
                OnrampAnalyticsEvent.Errors(
                    tokenSymbol = "tokenSymbol",
                    providerName = "providerName",
                    errorCode = error.code,
                ),
            )
            else -> { /* no-op */
            }
        }
        return error
    }
}
