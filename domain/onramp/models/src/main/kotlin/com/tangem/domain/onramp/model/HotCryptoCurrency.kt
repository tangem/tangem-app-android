package com.tangem.domain.onramp.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.Quote

/**
 * Hot crypto currency
 *
 * @property cryptoCurrency crypto currency
 * @property quote          quote
 *
[REDACTED_AUTHOR]
 */
data class HotCryptoCurrency(
    val cryptoCurrency: CryptoCurrency,
    val quote: Quote,
)