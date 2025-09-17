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
        if (country.code !in SEPA_AVAILABLE_COUNTRY_CODES) {
            return false
        }

        return Either.catch {
            repository.hasSepaMethod(
                userWallet = userWallet,
                currency = currency,
                country = country,
                cryptoCurrency = cryptoCurrency,
            )
        }.getOrElse { false }
    }

    companion object {
        val SEPA_AVAILABLE_COUNTRY_CODES = listOf(
            "AL", // Albania
            "AD", // Andorra
            "AT", // Austria
            "BE", // Belgium
            "BG", // Bulgaria
            "HR", // Croatia
            "CY", // Cyprus
            "CZ", // Czech Republic
            "DK", // Denmark
            "EE", // Estonia
            "FI", // Finland
            "FR", // France
            "DE", // Germany
            "GR", // Greece
            "HU", // Hungary
            "IS", // Iceland
            "IE", // Ireland
            "IT", // Italy
            "LV", // Latvia
            "LI", // Liechtenstein
            "LT", // Lithuania
            "LU", // Luxembourg
            "MT", // Malta
            "MD", // Moldova
            "MC", // Monaco
            "ME", // Montenegro
            "NL", // Netherlands
            "MK", // North Macedonia
            "NO", // Norway
            "PL", // Poland
            "PT", // Portugal
            "RO", // Romania
            "SM", // San Marino
            "RS", // Serbia
            "SK", // Slovakia
            "SI", // Slovenia
            "ES", // Spain
            "SE", // Sweden
            "CH", // Switzerland
            "GB", // United Kingdom
            "VA", // Vatican City
        )
    }
}