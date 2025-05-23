package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.SupportedLanguages

class GetTokenMarketInfoUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(
        appCurrency: AppCurrency,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
    ): Either<Unit, TokenMarketInfo> {
        return Either.catch {
            marketsTokenRepository.getTokenInfo(
                fiatCurrencyCode = appCurrency.code,
                tokenId = tokenId,
                tokenSymbol = tokenSymbol,
                languageCode = SupportedLanguages.getCurrentSupportedLanguageCode(),
            )
        }.mapLeft {}
    }
}