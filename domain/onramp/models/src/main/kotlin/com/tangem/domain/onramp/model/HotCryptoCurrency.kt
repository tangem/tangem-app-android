package com.tangem.domain.onramp.model

import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

/**
 * Hot crypto currency
 *
 * @property cryptoCurrency crypto currency
 * @property fiatRate       fiat rate
 * @property priceChange    price change
 *
[REDACTED_AUTHOR]
 */
data class HotCryptoCurrency(
    val cryptoCurrency: CryptoCurrency,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)