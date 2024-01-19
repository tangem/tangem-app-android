package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus

internal class TokenDetailsCurrencyStatusAnalyticsSender(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(maybeCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>) {
        val currencyStatus = maybeCurrencyStatus.getOrElse { return }
        val event = getEvent(currencyStatus)

        if (event != null) {
            analyticsEventHandler.send(event)
        }
    }

    private fun getEvent(currencyStatus: CryptoCurrencyStatus): AnalyticsEvent? {
        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Loading,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            is CryptoCurrencyStatus.NoQuote,
            -> null
            is CryptoCurrencyStatus.Unreachable -> TokenDetailsAnalyticsEvent.Notice.NetworkUnreachable(
                currency = currencyStatus.currency,
            )
        }
    }
}