package com.tangem.domain.tokens.operations

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class CurrencyStatusOperations(
    private val currency: CryptoCurrency,
    private val quote: Quote?,
    private val networkStatus: NetworkStatus?,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun createTokenStatus(): CryptoCurrencyStatus = withContext(dispatchers.default) {
        CryptoCurrencyStatus(
            currency = currency,
            value = createStatus(),
        )
    }

    private fun createStatus(): CryptoCurrencyStatus.Status {
        return when (val status = networkStatus?.value) {
            null -> CryptoCurrencyStatus.Loading
            is NetworkStatus.MissedDerivation -> CryptoCurrencyStatus.MissedDerivation
            is NetworkStatus.Unreachable -> CryptoCurrencyStatus.Unreachable
            is NetworkStatus.NoAccount -> CryptoCurrencyStatus.NoAccount
            is NetworkStatus.TransactionInProgress,
            is NetworkStatus.Verified,
            -> createStatus(
                amount = getTokenAmount(),
                hasTransactionsInProgress = status is NetworkStatus.TransactionInProgress,
            )
        }
    }

    private fun createStatus(amount: BigDecimal, hasTransactionsInProgress: Boolean): CryptoCurrencyStatus.Status {
        return when {
            currency is CryptoCurrency.Token && currency.isCustom -> CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quote?.fiatRate),
                fiatRate = quote?.fiatRate,
                priceChange = quote?.priceChange,
                hasTransactionsInProgress = hasTransactionsInProgress,
            )
            quote == null -> CryptoCurrencyStatus.Loading
            else -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quote.fiatRate),
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                hasTransactionsInProgress = hasTransactionsInProgress,
            )
        }
    }

    private fun getTokenAmount(): BigDecimal {
        val amount = networkStatus?.value?.amounts?.get(currency.id)
        return amount ?: error("Incorrect network status: $networkStatus")
    }

    private fun calculateFiatAmountOrNull(amount: BigDecimal, fiatRate: BigDecimal?): BigDecimal? {
        if (fiatRate == null) return null

        return calculateFiatAmount(amount, fiatRate)
    }

    private fun calculateFiatAmount(amount: BigDecimal, fiatRate: BigDecimal): BigDecimal {
        return amount * fiatRate
    }
}