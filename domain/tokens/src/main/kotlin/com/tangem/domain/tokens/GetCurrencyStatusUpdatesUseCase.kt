package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
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
 * Use case for fetching the status of a cryptocurrency associated with a user wallet.
 *
 * @property currenciesRepository Repository for managing and fetching cryptocurrencies.
 * @property quotesRepository Repository for managing and fetching cryptocurrency quotes.
 * @property networksRepository Repository for managing and fetching information related to blockchain networks.
 */
class GetCurrencyStatusUpdatesUseCase(
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
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(getCurrency(userWalletId, currencyId))
        }.flowOn(dispatchers.io)
    }

    private suspend fun getCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )

        return operations.getCurrencyStatusFlow(currencyId).map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}