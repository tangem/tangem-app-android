package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class GetFeePaidCryptoCurrencyStatusSyncUseCase(
    internal val currenciesRepository: CurrenciesRepository,
    internal val quotesRepository: QuotesRepository,
    internal val networksRepository: NetworksRepository,
    internal val stakingRepository: StakingRepository,
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<TokenListError, CryptoCurrencyStatus?> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, cryptoCurrency)
        val operations = CurrenciesStatusesOperations(
            userWalletId = userWalletId,
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
        )

        return either {
            when (feePaidCurrency) {
                FeePaidCurrency.Coin ->
                    operations
                        .getNetworkCoinSync(cryptoCurrency.network.id, cryptoCurrency.network.derivationPath)
                        .getOrNull()
                FeePaidCurrency.SameCurrency,
                is FeePaidCurrency.FeeResource,
                -> cryptoCurrencyStatus
                is FeePaidCurrency.Token -> operations.getCurrencyStatusSync(feePaidCurrency.tokenId).getOrNull()
            }
        }
    }
}