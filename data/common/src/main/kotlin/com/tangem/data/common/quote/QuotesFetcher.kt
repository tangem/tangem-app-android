package com.tangem.data.common.quote

import arrow.core.Either
import com.tangem.data.common.quote.utils.combine
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.models.QuotesResponse

/**
 * Fetcher of quotes
 *
[REDACTED_AUTHOR]
 *
 * @see <a href = "https://www.notion.so/tangem/Quotes-21b5d34eb6788038af9ccee37c7db7f9">Documentation<a/>
 */
interface QuotesFetcher {

    /**
     * Fetch
     *
     * @param fiatCurrencyId fiat currency id
     * @param currenciesIds  crypto currencies ids
     * @param fields         fields of [QuotesResponse.Quote]
     */
    suspend fun fetch(
        fiatCurrencyId: String,
        currenciesIds: Set<String>,
        fields: Set<Field>,
    ): Either<Error, QuotesResponse>

    /**
     * Fetch
     *
     * @param fiatCurrencyId fiat currency id
     * @param currencyId     crypto currencies ids
     * @param field          fields of [QuotesResponse.Quote]
     */
    suspend fun fetch(fiatCurrencyId: String, currencyId: String, field: Field): Either<Error, QuotesResponse> {
        return fetch(fiatCurrencyId = fiatCurrencyId, currenciesIds = setOf(currencyId), fields = setOf(field))
    }

    enum class Field(internal val value: String) {
        PRICE(value = "price"),
        PRICE_CHANGE_24H(value = "priceChange24h"),
        PRICE_CHANGE_1W(value = "priceChange1w"),
        PRICE_CHANGE_30D(value = "priceChange30d"),
        ALL_PRICES(
            value = setOf(PRICE, PRICE_CHANGE_24H, PRICE_CHANGE_1W, PRICE_CHANGE_30D).combine(),
        ),
        LAST_UPDATED_AT(value = "lastUpdatedAt"),
        ;
    }

    sealed interface Error {

        data object InvalidArgumentsError : Error

        data object CacheOperationError : Error

        data class ApiOperationError(val apiError: ApiResponseError) : Error
    }
}