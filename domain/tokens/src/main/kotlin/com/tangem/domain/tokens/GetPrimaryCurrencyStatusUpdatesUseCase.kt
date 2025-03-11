package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

/**
 * Use case for fetching the status of the primary cryptocurrency associated with a user wallet.
 *
 * @property dispatchers Provides coroutine dispatchers.
 */
class GetPrimaryCurrencyStatusUpdatesUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) {

    /**
     * Invokes the use case.
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    operator fun invoke(userWalletId: UserWalletId): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(getPrimaryCurrency(userWalletId))
        }.flowOn(dispatchers.io)
    }

    private suspend fun getPrimaryCurrency(
        userWalletId: UserWalletId,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return currencyStatusOperations.getPrimaryCurrencyStatusFlow(userWalletId).map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}