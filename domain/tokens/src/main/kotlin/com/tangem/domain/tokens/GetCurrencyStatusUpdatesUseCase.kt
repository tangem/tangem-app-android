package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
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
    private val stakingRepository: StakingRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Invokes the use case.
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @param currencyId The unique identifier of the cryptocurrency.
     * @param isSingleWalletWithTokens Indicates whether the user wallet contains only one token on card (old cards)
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(
                getCurrency(
                    userWalletId = userWalletId,
                    currencyId = currencyId,
                    isSingleWalletWithTokens = isSingleWalletWithTokens,
                ),
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            userWalletId = userWalletId,
        )

        val currencyFlow = if (isSingleWalletWithTokens) {
            operations.getCurrencyStatusSingleWalletWithTokensFlow(currencyId)
        } else {
            operations.getCurrencyStatusFlow(currencyId)
        }
        return currencyFlow.map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}