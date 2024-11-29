package com.tangem.domain.onramp

import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveDefaultCountryUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(country: OnrampCountry) {
        repository.saveDefaultCountry(country)
    }
}