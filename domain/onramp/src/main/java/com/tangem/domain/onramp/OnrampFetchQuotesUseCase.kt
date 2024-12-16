package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.CryptoCurrency

class OnrampFetchQuotesUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(amount: Amount, cryptoCurrency: CryptoCurrency) = Either.catch {
        repository.fetchQuotes(
            cryptoCurrency = cryptoCurrency,
            amount = amount,
        )
    }.mapLeft(errorResolver::resolve)
}