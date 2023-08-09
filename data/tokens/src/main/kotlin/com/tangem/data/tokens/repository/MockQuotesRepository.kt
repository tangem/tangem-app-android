package com.tangem.data.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.math.BigDecimal
import kotlin.random.Random

internal class MockQuotesRepository : QuotesRepository {

    override fun getQuotes(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Flow<Set<Quote>> {
        return channelFlow {
            val quotes = currenciesIds.map {
                Quote(
                    currencyId = it,
                    fiatRate = BigDecimal.ZERO,
                    priceChange = BigDecimal.ZERO,
                )
            }.toSet()

            delay(Random.nextLong(from = 200, until = 2_000))

            send(quotes)
        }
    }
}