package com.tangem.domain.tokens.operations

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
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
            is NetworkStatus.MissedDerivation -> createMissedDerivationStatus()
            is NetworkStatus.Unreachable -> createUnreachableStatus()
            is NetworkStatus.NoAccount -> createNoAccountStatus(status.amountToCreateAccount)
            is NetworkStatus.Verified -> createStatus(status)
        }
    }

    private fun createMissedDerivationStatus(): CryptoCurrencyStatus.MissedDerivation =
        CryptoCurrencyStatus.MissedDerivation(priceChange = quote?.priceChange, fiatRate = quote?.fiatRate)

    private fun createUnreachableStatus(): CryptoCurrencyStatus.Unreachable =
        CryptoCurrencyStatus.Unreachable(priceChange = quote?.priceChange, fiatRate = quote?.fiatRate)

    private fun createNoAccountStatus(amount: BigDecimal): CryptoCurrencyStatus.NoAccount =
        CryptoCurrencyStatus.NoAccount(
            amountToCreateAccount = amount,
            priceChange = quote?.priceChange,
            fiatRate = quote?.fiatRate,
        )

    private fun createStatus(status: NetworkStatus.Verified): CryptoCurrencyStatus.Status {
        val amount = status.amounts[currency.id]
            ?: return CryptoCurrencyStatus.Loading
        val hasCurrentNetworkTransactions = status.pendingTransactions.isNotEmpty()
        val currentTransactions = status.pendingTransactions.getOrElse(currency.id, ::emptySet)

        return when {
            ignoreQuote -> CryptoCurrencyStatus.NoQuote(
                amount = amount,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = status.address,
            )
            currency is CryptoCurrency.Token && currency.isCustom -> CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quote?.fiatRate),
                fiatRate = quote?.fiatRate,
                priceChange = quote?.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = status.address,
            )
            quote == null -> CryptoCurrencyStatus.Loading
            else -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quote.fiatRate),
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = status.address,
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
