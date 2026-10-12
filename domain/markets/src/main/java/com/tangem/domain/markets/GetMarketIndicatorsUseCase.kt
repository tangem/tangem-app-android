package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketIndicatorsRepository
import com.tangem.utils.SupportedLanguages

/**
 * Use case for getting the global market indicators of the "Total market cap" block
 *
 * @property marketIndicatorsRepository repository of market indicators
 */
class GetMarketIndicatorsUseCase(
    private val marketIndicatorsRepository: MarketIndicatorsRepository,
) {

    suspend operator fun invoke(appCurrency: AppCurrency): Either<Throwable, MarketIndicators> {
        return marketIndicatorsRepository.getMarketIndicators(
            fiatCurrencyCode = appCurrency.code,
            languageCode = SupportedLanguages.getCurrentSupportedLanguageCode(),
        )
    }
}