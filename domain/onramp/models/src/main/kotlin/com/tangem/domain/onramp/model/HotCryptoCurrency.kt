package com.tangem.domain.onramp.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus

/**
 * Hot crypto currency
 *
 * @property cryptoCurrency crypto currency
 * @property quoteStatus    quote status
 *
[REDACTED_AUTHOR]
 */
data class HotCryptoCurrency(
    val cryptoCurrency: CryptoCurrency,
    val quoteStatus: QuoteStatus,
)