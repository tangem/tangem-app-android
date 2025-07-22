package com.tangem.data.quotes.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.l2BlockchainsCoinIds
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.datasource.api.tangemTech.models.QuotesResponse

/** Adapter to replace unsupported currencies for quotes request if it necessary */
internal object QuotesUnsupportedCurrenciesIdAdapter {

    /** Ethereum coin ID */
    private val ethCoinId = Blockchain.Ethereum.toCoinId()

    /** Map that contains unsupported currencies and their replacement for request */
    private val UNSUPPORTED_IDS_WITH_REPLACEMENTS = l2BlockchainsCoinIds.associateWith { ethCoinId }

    /** Replaces unsupported currencies id [currenciesIds] to it replacements for request */
    fun replaceUnsupportedCurrencies(currenciesIds: Set<String>): ReplacementResult {
        val idsForRequest = mutableSetOf<String>()
        val idsFiltered = mutableSetOf<String>()

        currenciesIds.forEach { currencyId ->
            val replacementId = UNSUPPORTED_IDS_WITH_REPLACEMENTS[currencyId]

            if (replacementId != null) {
                idsFiltered.add(currencyId)
                idsForRequest.add(replacementId)
            } else {
                idsForRequest.add(currencyId)
            }
        }

        return ReplacementResult(idsForRequest = idsForRequest, idsFiltered = idsFiltered)
    }

    /** Recover previously replaced currencies [filteredIds] to [response] that uses in application */
    fun getResponseWithUnsupportedCurrencies(response: QuotesResponse, filteredIds: Set<String>): QuotesResponse {
        val updatedQuotes = mutableMapOf<String, QuotesResponse.Quote>()

        filteredIds.forEach { filteredId ->
            val replacementId = UNSUPPORTED_IDS_WITH_REPLACEMENTS[filteredId]

            if (replacementId != null) {
                val quoteReplacement = response.quotes[replacementId]
                if (quoteReplacement != null) {
                    updatedQuotes[filteredId] = quoteReplacement
                }
            }
        }

        return response.copy(quotes = updatedQuotes + response.quotes)
    }

    data class ReplacementResult(val idsForRequest: Set<String>, val idsFiltered: Set<String>)
}