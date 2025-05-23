package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.wallets.models.UserWallet

class OnrampFetchQuotesUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(userWallet: UserWallet, amount: Amount, cryptoCurrency: CryptoCurrency) = Either.catch {
        repository.fetchQuotes(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            amount = amount,
        )
    }.mapLeft(errorResolver::resolve)
}