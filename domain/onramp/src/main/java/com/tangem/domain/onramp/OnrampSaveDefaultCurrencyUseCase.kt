package com.tangem.domain.onramp

import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveDefaultCurrencyUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(currency: OnrampCurrency) {
        repository.saveDefaultCurrency(currency)
    }
}