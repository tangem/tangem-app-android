package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.repositories.OnrampRepository

class CheckOnrampAvailabilityUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(): Either<Throwable, OnrampAvailability> {
        return Either.catch {
            repository.fetchPaymentMethodsIfAbsent()
            val savedCountry = repository.getDefaultCountrySync()
            if (savedCountry != null) {
                proceedWithSavedCountry(savedCountry = savedCountry)
            } else {
                val detectedCountry = repository.getCountryByIp()
                OnrampAvailability.ConfirmResidency(detectedCountry)
            }
        }
    }

    private suspend fun proceedWithSavedCountry(savedCountry: OnrampCountry): OnrampAvailability {
        val countries = repository.getCountries()
        val onrampAvailable = countries.find { it == savedCountry }?.onrampAvailable ?: false
        return if (onrampAvailable) {
            val currency = repository.getDefaultCurrencySync() ?: run {
                repository.saveDefaultCurrency(savedCountry.defaultCurrency)
                savedCountry.defaultCurrency
            }
            OnrampAvailability.Available(country = savedCountry, currency = currency)
        } else {
            OnrampAvailability.NotSupported(savedCountry)
        }
    }
}