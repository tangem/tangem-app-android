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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
[REDACTED_AUTHOR]
 */
class GetAllWalletsCryptoCurrencyStatusesUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(
        currencyRawId: String,
    ): Flow<Map<UserWallet, Flow<List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>>> {
        return currenciesRepository.getAllWalletsCryptoCurrencies(currencyRawId)
            .map { userWalletsWithCurrencies: Map<UserWallet, List<CryptoCurrency>> ->
                userWalletsWithCurrencies
                    .mapValues {
                        val operations = CurrenciesStatusesOperations(
                            userWalletId = it.key.walletId,
                            currenciesRepository = currenciesRepository,
                            quotesRepository = quotesRepository,
                            networksRepository = networksRepository,
                            stakingRepository = stakingRepository,
                        )

                        val currencyStatuses = it.value
                            .map(operations::getCurrencyStatusFlow)
                            .let {
                                combine(*it.toTypedArray()) { a ->
                                    a.map { maybeCurrency ->
                                        maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
                                    }
                                }
                            }

                        currencyStatuses
                    }
            }
        // .map {
        //     it.values.
        // }
        // .flatMapLatest { a: Map<UserWallet, Flow<List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>> ->
        //
        // }
    }
}