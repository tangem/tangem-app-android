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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
[REDACTED_AUTHOR]
 */
class GetAllWalletsCryptoCurrencyStatusesUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        currencyRawId: String,
    ): Flow<Map<UserWallet, List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>> {
        return currenciesRepository.getAllWalletsCryptoCurrencies(currencyRawId)
            .flatMapConcat { userWalletsWithCurrencies: Map<UserWallet, List<CryptoCurrency>> ->
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

                    combine(currencyStatusFlows) { statuses ->
                        userWallet to statuses.toList()
                    }
                }

                combine(walletStatusFlows) { it.toMap() }
            }
    }
}