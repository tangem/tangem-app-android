package com.tangem.domain.tokens.operations

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import com.tangem.domain.core.raise.DelegatedRaise
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class CurrencyStatusOperations<OtherError>(
    private val currency: CryptoCurrency,
    private val quote: Quote?,
    private val networkStatus: NetworkStatus?,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<OtherError>,
    transformError: (Error) -> OtherError,
) : DelegatedRaise<CurrencyStatusOperations.Error, OtherError>(raise, transformError) {

    suspend fun createTokenStatus(): CryptoCurrencyStatus = withContext(dispatchers.default) {
        CryptoCurrencyStatus(currency, createStatus())
    }

    private fun createStatus(): CryptoCurrencyStatus.Status {
        return when (val status = networkStatus?.value) {
            null -> CryptoCurrencyStatus.Loading
            is NetworkStatus.MissedDerivation -> CryptoCurrencyStatus.MissedDerivation
            is NetworkStatus.Unreachable -> CryptoCurrencyStatus.Unreachable
            is NetworkStatus.NoAccount -> CryptoCurrencyStatus.NoAccount
            is NetworkStatus.Verified -> createStatus(status)
        }
    }

    private fun createStatus(status: NetworkStatus.Verified): CryptoCurrencyStatus.Status {
        val amount = ensureNotNull(status.amounts[currency.id]) {
            Error.UnableToFindAmount(currency.id)
        }

        return when {
            currency is CryptoCurrency.Token && currency.isCustom -> CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quote?.fiatRate),
                fiatRate = quote?.fiatRate,
                priceChange = quote?.priceChange,
                hasTransactionsInProgress = status.hasTransactionsInProgress,
            )
            quote == null -> CryptoCurrencyStatus.Loading
            else -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quote.fiatRate),
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                hasTransactionsInProgress = status.hasTransactionsInProgress,
            )
        }
    }

    private fun calculateFiatAmountOrNull(amount: BigDecimal, fiatRate: BigDecimal?): BigDecimal? {
        if (fiatRate == null) return null

        return calculateFiatAmount(amount, fiatRate)
    }

    private fun calculateFiatAmount(amount: BigDecimal, fiatRate: BigDecimal): BigDecimal {
        return amount * fiatRate
    }

    sealed class Error {

        data class UnableToFindAmount(val currencyId: CryptoCurrency.ID) : Error()
    }
}
