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
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Get crypto currency statuses by raw ID for all wallets
 *
 * @property currenciesRepository currencies repository
 * @property quotesRepository     quotes repository
 * @property networksRepository   networks repository
 * @property stakingRepository    staking repository
 * @property dispatchers          dispatchers
 *
[REDACTED_AUTHOR]
 */
class GetAllWalletsCryptoCurrencyStatusesUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Get crypto currency statuses by [currencyRawId] for all wallets
     *
     * @param currencyRawId currency raw ID
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        currencyRawId: CryptoCurrency.RawID,
        needFilterByAvailable: Boolean = false,
    ): Flow<Map<UserWallet, List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>> {
        return currenciesRepository.getAllWalletsCryptoCurrencies(currencyRawId, needFilterByAvailable)
            .flatMapLatest { userWalletsWithCurrencies: Map<UserWallet, List<CryptoCurrency>> ->
                val walletStatusFlows = userWalletsWithCurrencies.map { (userWallet, cryptoCurrencies) ->
                    val operations = CurrenciesStatusesOperations(
                        userWalletId = userWallet.walletId,
                        currenciesRepository = currenciesRepository,
                        quotesRepository = quotesRepository,
                        networksRepository = networksRepository,
                        stakingRepository = stakingRepository,
                    )

                    val currencyStatusFlows = cryptoCurrencies.map { cryptoCurrency ->
                        operations.getCurrencyStatusFlow(cryptoCurrency)
                            .map { it.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError) }
                    }

                    combine(currencyStatusFlows) { statuses -> userWallet to statuses.toList() }
                        .onEmpty { emit(userWallet to emptyList()) }
                }

                combine(walletStatusFlows) { it.toMap() }
                    .onEmpty { emit(emptyMap()) }
            }
            .flowOn(dispatchers.io)
    }
}