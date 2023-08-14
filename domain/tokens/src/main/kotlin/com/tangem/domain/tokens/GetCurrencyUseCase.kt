package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

/**
 * Use case for fetching the status of a specific cryptocurrency associated with a user wallet.
 *
 * @property currenciesRepository Repository for managing and fetching cryptocurrencies.
 * @property quotesRepository Repository for managing and fetching cryptocurrency quotes.
 * @property networksRepository Repository for managing and fetching information related to blockchain networks.
 * @property dispatchers Provides coroutine dispatchers.
 */
class GetCurrencyUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Invokes the use case.
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @param currencyId The unique identifier of the cryptocurrency.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting either a [CurrencyError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        refresh: Boolean = false,
    ): Flow<Either<CurrencyError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(getCurrency(userWalletId, currencyId, refresh))
        }.flowOn(dispatchers.io)
    }

    private suspend fun getCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        refresh: Boolean,
    ): Flow<Either<CurrencyError, CryptoCurrencyStatus>> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
            refresh = refresh,
        )

        return operations.getCurrencyStatusFlow(currencyId).map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}