package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.CryptoCurrency

class CheckOnrampAvailabilityUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(cryptoCurrency: CryptoCurrency): Either<Throwable, OnrampAvailability> {
        return Either.catch {
            repository.fetchPaymentMethodsIfAbsent()
            repository.getDefaultCountrySync()?.let { savedCountry ->
                return@catch if (savedCountry.onrampAvailable) {
                    val currency = repository.getDefaultCurrencySync() ?: run {
                        repository.saveDefaultCurrency(savedCountry.defaultCurrency)
                        savedCountry.defaultCurrency
                    }
                    repository.fetchPairs(
                        currency = currency,
                        country = savedCountry,
                        cryptoCurrency = cryptoCurrency,
                    )
                    OnrampAvailability.Available(country = savedCountry, currency = currency)
                } else {
                    OnrampAvailability.NotSupported(savedCountry)
                }
            }

            val detectedCountry = repository.getCountryByIp()
            OnrampAvailability.ConfirmResidency(detectedCountry)
        }
    }
}
