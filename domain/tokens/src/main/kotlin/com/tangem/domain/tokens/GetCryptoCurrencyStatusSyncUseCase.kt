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

class GetCryptoCurrencyStatusSyncUseCase(
    internal val currenciesRepository: CurrenciesRepository,
    internal val quotesRepository: QuotesRepository,
    internal val networksRepository: NetworksRepository,
    internal val stakingRepository: StakingRepository,
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean = false,
    ): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        val operations = CurrenciesStatusesOperations(
            userWalletId = userWalletId,
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
        )

        return operations.getCurrencyStatusSync(cryptoCurrencyId, isSingleWalletWithTokens)
            .mapLeft { error -> error.mapToCurrencyError() }
    }

    suspend operator fun invoke(userWalletId: UserWalletId): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        val operations = CurrenciesStatusesOperations(
            userWalletId = userWalletId,
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
        )

        return operations.getPrimaryCurrencyStatusSync()
            .mapLeft { error -> error.mapToCurrencyError() }
    }
}
