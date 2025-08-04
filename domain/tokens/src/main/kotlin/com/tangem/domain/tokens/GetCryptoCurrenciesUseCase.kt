package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.models.wallet.UserWalletId

class GetCryptoCurrenciesUseCase(
    private val currenciesRepository: CurrenciesRepository,
) {

    /**
     * Retrieves the list of cryptocurrencies within a multi-currency wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     *
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<CurrencyStatusError, List<CryptoCurrency>> {
        return Either.catch {
            currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        }.mapLeft(CurrencyStatusError::DataError)
    }
}