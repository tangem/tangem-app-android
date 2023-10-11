package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

class GetNetworkCoinStatusUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(
                flow = getCurrency(
                    userWalletId = userWalletId,
                    networkId = networkId,
                    derivationPath = derivationPath,
                    isSingleWalletWithTokens = isSingleWalletWithTokens,
                ),
            )
        }
            .flowOn(dispatchers.io)
    }

    private suspend fun getCurrency(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )
        val networkFlow = if (isSingleWalletWithTokens) {
            operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
        } else {
            operations.getNetworkCoinFlow(networkId, derivationPath)
        }
        return networkFlow.map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}
