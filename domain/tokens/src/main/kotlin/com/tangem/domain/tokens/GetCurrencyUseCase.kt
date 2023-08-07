package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.error.mapper.mapToTokenError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

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
        return channelFlow {
            recover(
                block = {
                    getCurrency(userWalletId, currencyId, refresh).collectLatest { currencyStatus ->
                        send(currencyStatus.right())
                    }
                },
                recover = { error ->
                    send(error.left())
                },
            )
        }
    }

    private suspend fun Raise<CurrencyError>.getCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        refresh: Boolean,
    ): Flow<CryptoCurrencyStatus> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
            refresh = refresh,
            dispatchers = dispatchers,
            raise = this,
            transformError = CurrenciesStatusesOperations.Error::mapToTokenError,
        )

        return operations.getCurrencyStatusFlow(currencyId)
    }
}
