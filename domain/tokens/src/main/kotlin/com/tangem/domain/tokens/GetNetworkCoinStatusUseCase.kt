package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

class GetNetworkCoinStatusUseCase(
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
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

    suspend fun invokeSync(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        val maybeCurrency = if (isSingleWalletWithTokens) {
            currencyStatusOperations.getNetworkCoinForSingleWalletWithTokenSync(userWalletId, networkId)
        } else {
            currencyStatusOperations.getNetworkCoinSync(userWalletId, networkId, derivationPath)
        }
        return maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
    }

    private suspend fun getCurrency(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        val networkFlow = if (isSingleWalletWithTokens) {
            currencyStatusOperations.getNetworkCoinForSingleWalletWithTokenFlow(userWalletId, networkId)
        } else {
            currencyStatusOperations.getNetworkCoinFlow(userWalletId, networkId, derivationPath)
        }
        return networkFlow.map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}