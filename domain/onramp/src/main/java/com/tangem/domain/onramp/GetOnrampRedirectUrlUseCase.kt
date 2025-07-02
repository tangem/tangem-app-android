package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.wallets.models.UserWallet

class GetOnrampRedirectUrlUseCase(
    private val repository: OnrampRepository,
    private val transactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        quote: OnrampProviderWithQuote.Data,
        cryptoCurrency: CryptoCurrency,
        isDarkTheme: Boolean,
    ): Either<OnrampError, OnrampTransaction> {
        return Either.catch {
            val transaction = repository.getOnrampData(
                userWallet = userWallet,
                cryptoCurrency = cryptoCurrency,
                quote = quote,
                isDarkTheme = isDarkTheme,
            )
            transactionRepository.storeTransaction(transaction)
            transaction
        }.mapLeft(errorResolver::resolve)
    }
}