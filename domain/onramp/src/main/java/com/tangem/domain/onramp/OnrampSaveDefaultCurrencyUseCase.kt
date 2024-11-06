package com.tangem.domain.onramp

import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveDefaultCurrencyUseCase(private val onrampRepository: OnrampRepository) {

    suspend operator fun invoke(currency: OnrampCurrency) {
        onrampRepository.saveDefaultCurrency(currency)
    }
}
