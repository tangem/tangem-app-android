package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class GetOnrampTransactionsUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Either<OnrampError, Flow<List<OnrampTransaction>>> {
        return Either.catch {
            onrampTransactionRepository.getTransactions(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrencyId,
            )
        }.mapLeft {
            OnrampError.UnknownError
        }
    }
}
