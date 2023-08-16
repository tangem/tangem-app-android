package com.tangem.domain.tokens.operations

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Quote
import java.math.BigDecimal

internal class CurrencyStatusOperations(
    private val currency: CryptoCurrency,
    private val quote: Quote?,
    private val networkStatus: NetworkStatus?,
    private val ignoreQuote: Boolean,
) {

    fun createTokenStatus(): CryptoCurrencyStatus = CryptoCurrencyStatus(currency, createStatus())

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
        val amount = status.amounts[currency.id] ?: return CryptoCurrencyStatus.Unreachable
        val hasCurrentNetworkTransactions = status.currentTransactions.isNotEmpty()
        val currentTransactions = status.currentTransactions.getOrElse(currency.id, ::emptySet)

        return when {
            ignoreQuote -> CryptoCurrencyStatus.NoQuote(
                amount = amount,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                currentTransactions = currentTransactions,
            )
            currency is CryptoCurrency.Token && currency.isCustom -> CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quote?.fiatRate),
                fiatRate = quote?.fiatRate,
                priceChange = quote?.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                currentTransactions = currentTransactions,
            )
            quote == null -> CryptoCurrencyStatus.Loading
            else -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quote.fiatRate),
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                currentTransactions = currentTransactions,
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
}
