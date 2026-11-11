package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class CheckOnrampAvailabilityUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        isCountryByIpEnabled: Boolean = false,
    ): Either<OnrampError, OnrampAvailability> {
        return Either.catch {
            repository.fetchPaymentMethodsIfAbsent(userWallet)
            if (isCountryByIpEnabled) {
                checkWithCountryByIp(userWallet)
            } else {
                checkWithResidencyConfirmation(userWallet)
            }
        }.mapLeft(errorResolver::resolve)
    }

    private suspend fun checkWithCountryByIp(userWallet: UserWallet): OnrampAvailability {
        val savedCountry = repository.getDefaultCountrySync()
        val country = savedCountry ?: detectCountry(userWallet)
        val listed = repository.fetchCountries(userWallet).find { it.id == country.id }
        val validated = listed?.copy(defaultCurrency = country.defaultCurrency) ?: country
        if (validated != savedCountry) repository.saveDefaultCountry(validated)
        return resolveAvailability(validated)
    }

    private suspend fun detectCountry(userWallet: UserWallet): OnrampCountry {
        val detectedCountry = repository.getCountryByIp(userWallet)
        val currency = repository.getSelectedCurrencySync() ?: detectedCountry.defaultCurrency
        return detectedCountry.copy(defaultCurrency = currency)
    }

    private suspend fun resolveAvailability(country: OnrampCountry): OnrampAvailability {
        return if (country.isOnrampAvailable) {
            val currency = repository.getSelectedCurrencySync() ?: country.defaultCurrency
            OnrampAvailability.Available(country = country, currency = currency)
        } else {
            OnrampAvailability.NotSupported(country)
        }
    }

    private suspend fun checkWithResidencyConfirmation(userWallet: UserWallet): OnrampAvailability {
        val savedCountry = repository.getDefaultCountrySync()
        return if (savedCountry != null) {
            proceedWithSavedCountry(userWallet = userWallet, savedCountry = savedCountry)
        } else {
            val detectedCountry = repository.getCountryByIp(userWallet)
            OnrampAvailability.ConfirmResidency(detectedCountry)
        }
    }

    private suspend fun proceedWithSavedCountry(
        userWallet: UserWallet,
        savedCountry: OnrampCountry,
    ): OnrampAvailability {
        val countries = repository.fetchCountries(userWallet)
        val updatedCountry = countries.find { it.id == savedCountry.id } ?: savedCountry
        return if (updatedCountry.isOnrampAvailable) {
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