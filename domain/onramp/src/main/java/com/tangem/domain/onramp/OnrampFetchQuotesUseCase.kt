package com.tangem.domain.onramp

import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.CryptoCurrency

class OnrampFetchQuotesUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(amount: Amount, cryptoCurrency: CryptoCurrency) {
        repository.fetchQuotes(
            cryptoCurrency = cryptoCurrency,
            amount = amount,
        )
    }
}