package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Get crypto currency statuses by raw ID for all wallets
 *
 * @property currenciesRepository currencies repository
 * @property dispatchers          dispatchers
 *
[REDACTED_AUTHOR]
 */
class GetAllWalletsCryptoCurrencyStatusesUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
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
                    val currencyStatusFlows = cryptoCurrencies.map { cryptoCurrency ->
                        currencyStatusOperations.getCurrencyStatusFlow(userWallet.walletId, cryptoCurrency)
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