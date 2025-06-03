package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.models.currency.CryptoCurrency

class GetTokenPriceChartUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(
        appCurrency: AppCurrency,
        interval: PriceChangeInterval,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
        preview: Boolean,
    ): Either<Unit, TokenChart> {
        return Either.catch {
            if (preview) {
                marketsTokenRepository.getChartPreview(
                    fiatCurrencyCode = appCurrency.code,
                    interval = interval,
                    tokenId = tokenId,
                    tokenSymbol = tokenSymbol,
                )
            } else {
                marketsTokenRepository.getChart(
                    fiatCurrencyCode = appCurrency.code,
                    interval = interval,
                    tokenId = tokenId,
                    tokenSymbol = tokenSymbol,
                )
            }
        }.mapLeft {}
    }
}