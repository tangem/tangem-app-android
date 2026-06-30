package com.tangem.domain.quotes

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

/**
 * Checks whether a network fee is higher than a single hardcoded USD threshold, applied uniformly
 * across all networks. The fee USD value is computed from the fee currency's USD quote
 * ([GetCurrencyUSDQuoteUseCase]), independent of the user's selected app currency.
 *
 * Returns `false` when there is no USD quote or no raw currency id — never warn without pricing data.
 */
class IsHighNetworkFeeUseCase(
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase,
) {

    suspend operator fun invoke(feeCurrency: CryptoCurrency, feeAmount: BigDecimal): Boolean {
        val rawCurrencyId = feeCurrency.id.rawCurrencyId ?: return false
        val usdRate = getCurrencyUSDQuoteUseCase(rawCurrencyId) ?: return false

        return feeAmount.multiply(usdRate) > HIGH_FEE_USD_THRESHOLD
    }

    private companion object {
        val HIGH_FEE_USD_THRESHOLD = BigDecimal("10")
    }
}