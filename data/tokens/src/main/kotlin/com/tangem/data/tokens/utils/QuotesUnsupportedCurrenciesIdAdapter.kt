package com.tangem.data.tokens.utils

import com.tangem.datasource.api.tangemTech.models.QuotesResponse

/**
 * Adapter to replace unsupported currencies for quotes request if it necessary
 */
class QuotesUnsupportedCurrenciesIdAdapter {

    /**
     * Replaces unsupported currencies id to it replacements for request
     */
    fun replaceUnsupportedCurrencies(currenciesIds: Set<String>): ReplacementResult {
        val idsForRequest = mutableSetOf<String>()
        val idsFiltered = mutableSetOf<String>()
        currenciesIds.forEach { currencyId ->
            unsupportedMapWithReplacement[currencyId]?.let {
                idsFiltered.add(currencyId)
                idsForRequest.add(it)
            } ?: idsForRequest.add(currencyId)
        }
        return ReplacementResult(idsForRequest = idsForRequest, idsFiltered = idsFiltered)
    }

    /**
     * Recover previously replaced currencies to response that uses in application
     */
    fun getResponseWithUnsupportedCurrencies(response: QuotesResponse, filteredIds: Set<String>): QuotesResponse {
        val updatedQuotes = mutableMapOf<String, QuotesResponse.Quote>()
        filteredIds.forEach { filteredId ->
            unsupportedMapWithReplacement[filteredId]?.let { baseId ->
                response.quotes[baseId]?.let { quote ->
                    updatedQuotes[filteredId] = quote
                }
            }
        }
        return response.copy(quotes = updatedQuotes + response.quotes)
    }

    data class ReplacementResult(val idsForRequest: Set<String>, val idsFiltered: Set<String>)

    companion object {
        /**
         * Map that contains unsupported currencies and their replacement for request
         */
        private val unsupportedMapWithReplacement = mapOf(
            "optimistic-ethereum" to "ethereum",
            "arbitrum-one" to "ethereum",
        )
    }
}