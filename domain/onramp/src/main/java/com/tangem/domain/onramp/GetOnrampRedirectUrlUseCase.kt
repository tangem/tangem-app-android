package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

class GetOnrampRedirectUrlUseCase(
    private val repository: OnrampRepository,
    private val transactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        quote: OnrampProviderWithQuote.Data,
        cryptoCurrency: CryptoCurrency,
    ): Either<Throwable, String> {
        return Either.catch {
            val transaction = repository.getOnrampData(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                quote = quote,
            )
            transactionRepository.storeTransaction(transaction)
            transaction.redirectUrl
        }
    }
}