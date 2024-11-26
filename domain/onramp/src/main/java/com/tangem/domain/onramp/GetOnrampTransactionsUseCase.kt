package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty

class GetOnrampTransactionsUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<Either<OnrampError, List<OnrampTransaction>>> {
        return onrampTransactionRepository.getTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyId = cryptoCurrencyId,
        ).map { it.right() }
            .catch { OnrampError.UnknownError.left() }
            .onEmpty { OnrampError.UnknownError.left() }
    }
}
