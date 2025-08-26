package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency

class OnrampSepaAvailableUseCase(
    private val repository: OnrampRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        currency: OnrampCurrency,
        country: OnrampCountry,
        cryptoCurrency: CryptoCurrency,
    ): Boolean {
        return Either.catch {
            repository.hasMercuryoSepaMethod(
                userWallet = userWallet,
                currency = currency,
                country = country,
                cryptoCurrency = cryptoCurrency,
            )
        }.getOrElse { false }
    }
}