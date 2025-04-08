package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.onramp.repositories.LegacyTopUpRepository
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus

class GetLegacyTopUpUrlUseCase(
    private val legacyTopUpRepository: LegacyTopUpRepository,
) {

    suspend operator fun invoke(cryptoCurrencyStatus: CryptoCurrencyStatus): Either<OnrampError.DomainError, String> =
        either {
            val walletAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
                ?: raise(OnrampError.DomainError("Wallet address is null"))
            legacyTopUpRepository.getTopUpUrl(
                cryptoCurrency = cryptoCurrencyStatus.currency,
                walletAddress = walletAddress,
            )
        }
}