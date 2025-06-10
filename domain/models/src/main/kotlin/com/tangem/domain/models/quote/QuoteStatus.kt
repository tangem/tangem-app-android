package com.tangem.domain.models.quote

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

/**
 * Represents the status of a specific currency quote
 *
 * @property rawCurrencyId the unique identifier of the cryptocurrency for which the financial information is provided
 * @property value         the specific status value
 */
data class QuoteStatus(val rawCurrencyId: CryptoCurrency.RawID, val value: Value) {

    /** Constructor for creating the [Empty] status of a specific currency quote */
    constructor(rawCurrencyId: CryptoCurrency.RawID) : this(rawCurrencyId = rawCurrencyId, value = Empty)

    /** Represents the various possible statuses of a quote */
    sealed interface Value {

        /** Status source */
        val source: StatusSource

        fun copySealed(source: StatusSource): Value {
            return when (this) {
                is Empty -> this
                is Data -> copy(source = source)
            }
        }
    }

    /** Represents unknown financial information for a specific cryptocurrency */
    data object Empty : Value {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents financial information for a specific cryptocurrency, including its fiat exchange rate and
     * price change.
     *
     * @property source      status source
     * @property fiatRate    the current fiat exchange rate for the cryptocurrency
     * @property priceChange the price change for the cryptocurrency
     */
    data class Data(
        override val source: StatusSource,
        val fiatRate: BigDecimal,
        val priceChange: BigDecimal,
    ) : Value
}

/** Applies the given [function] if [QuoteStatus.Value] is [QuoteStatus.Data] or does nothing */
inline fun QuoteStatus.mapData(function: QuoteStatus.Data.() -> Unit): QuoteStatus {
    if (value is QuoteStatus.Data) value.function()

    return this
}

/**
 * Applies the given [onData] or [onEmpty] functions depending on [QuoteStatus.Data] or [QuoteStatus.Empty]
 *
 * @param T type of result
 */
inline fun <T> QuoteStatus.fold(onData: QuoteStatus.Data.() -> T, onEmpty: QuoteStatus.Empty.() -> T): T {
    return when (value) {
        is QuoteStatus.Data -> value.onData()
        is QuoteStatus.Empty -> value.onEmpty()
    }
}