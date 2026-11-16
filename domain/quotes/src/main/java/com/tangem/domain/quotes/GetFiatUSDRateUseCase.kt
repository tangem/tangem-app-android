package com.tangem.domain.quotes

import java.math.BigDecimal

/** Returns how many USD one unit of a fiat currency is worth, or null if the rate is unavailable */
class GetFiatUSDRateUseCase(
    private val quotesRepository: QuotesRepository,
) {

    suspend operator fun invoke(fiatCurrencyCode: String): BigDecimal? {
        if (fiatCurrencyCode.equals(USD_CODE, ignoreCase = true)) return BigDecimal.ONE

        return quotesRepository.getFiatUSDRate(fiatCurrencyCode)
    }

    private companion object {
        const val USD_CODE = "USD"
    }
}