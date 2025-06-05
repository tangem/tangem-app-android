package com.tangem.domain.tokens.model

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

sealed interface Quote {

    val rawCurrencyId: CryptoCurrency.RawID

    fun copySealed(source: StatusSource): Quote {
        return when (this) {
            is Empty -> this
            is Value -> copy(source = source)
        }
    }

    /**
     * Represents unknown financial information for a specific cryptocurrency.
     *
     * @property rawCurrencyId The raw cryptocurrency ID.
     */
    data class Empty(override val rawCurrencyId: CryptoCurrency.RawID) : Quote

    /**
     * Represents financial information for a specific cryptocurrency, including its fiat exchange rate and price change.
     *
     * @property rawCurrencyId The unique identifier of the cryptocurrency for which the financial information is provided.
     * @property fiatRate The current fiat exchange rate for the cryptocurrency.
     * @property priceChange The price change for the cryptocurrency.
     * @property source source of data
     */
    data class Value(
        override val rawCurrencyId: CryptoCurrency.RawID,
        val fiatRate: BigDecimal,
        val priceChange: BigDecimal,
        val source: StatusSource,
    ) : Quote
}