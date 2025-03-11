package com.tangem.features.onramp.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.error.OnrampError

internal fun AnalyticsEventHandler.sendOnrampErrorEvent(
    error: OnrampError,
    tokenSymbol: String,
    providerName: String? = null,
    paymentMethod: String? = null,
) = when (error) {
    is OnrampError.DataError -> send(
        OnrampAnalyticsEvent.Errors(
            tokenSymbol = tokenSymbol,
            providerName = providerName,
            paymentMethod = paymentMethod,
            errorCode = error.code,
        ),
    )
    is OnrampError.DomainError -> {
        send(
            OnrampAnalyticsEvent.AppErrors(
                tokenSymbol = tokenSymbol,
                errorDescription = error.description.orEmpty(),
            ),
        )
    }
    is OnrampError.AmountError.TooBigError,
    is OnrampError.AmountError.TooSmallError,
    OnrampError.RedirectError.VerificationFailed,
    OnrampError.RedirectError.WrongRequestId,
    OnrampError.PairsNotFound,
    -> { /* no-op */ }
}