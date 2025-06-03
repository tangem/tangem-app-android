package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.wallets.models.UserWallet

class CheckOnrampAvailabilityUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<OnrampError, OnrampAvailability> {
        return Either.catch {
            repository.fetchPaymentMethodsIfAbsent(userWallet)
            val savedCountry = repository.getDefaultCountrySync()
            if (savedCountry != null) {
                proceedWithSavedCountry(userWallet = userWallet, savedCountry = savedCountry)
            } else {
                val detectedCountry = repository.getCountryByIp(userWallet)
                OnrampAvailability.ConfirmResidency(detectedCountry)
            }
        }.mapLeft(errorResolver::resolve)
    }

    private suspend fun proceedWithSavedCountry(
        userWallet: UserWallet,
        savedCountry: OnrampCountry,
    ): OnrampAvailability {
        val countries = repository.fetchCountries(userWallet)
        val updatedCountry = countries.find { it.id == savedCountry.id } ?: savedCountry
        return if (updatedCountry.onrampAvailable) {
            val currency = repository.getDefaultCurrencySync() ?: run {
                repository.saveDefaultCurrency(savedCountry.defaultCurrency)
                savedCountry.defaultCurrency
            }
            OnrampAvailability.Available(country = savedCountry, currency = currency)
        } else {
            OnrampAvailability.NotSupported(updatedCountry)
        }
    }
}