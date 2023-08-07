package com.tangem.data.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal

internal class MockQuotesRepository : QuotesRepository {

    override fun getQuotes(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Flow<Set<Quote>> {
        return flowOf(
            currenciesIds.map {
                Quote(
                    currencyId = it,
                    fiatRate = BigDecimal.ZERO,
                    priceChange = BigDecimal.ZERO,
                )
            }.toSet(),
        )
    }
}
