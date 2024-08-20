package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository

class GetTokenPriceChartUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(
        appCurrency: AppCurrency,
        interval: PriceChangeInterval,
        tokenId: String,
    ): Either<Unit, TokenChart> {
        return Either.catch {
            marketsTokenRepository.getChart(
                fiatCurrencyCode = appCurrency.code,
                interval = interval,
                tokenId = tokenId,
            )
        }.mapLeft {}
    }
}
