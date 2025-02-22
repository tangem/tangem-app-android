package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetFeePaidCryptoCurrencyStatusSyncUseCase(
    internal val currenciesRepository: CurrenciesRepository,
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<TokenListError, CryptoCurrencyStatus?> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val network = cryptoCurrency.network
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, network)

        return either {
            when (feePaidCurrency) {
                is FeePaidCurrency.Coin -> {
                    currencyStatusOperations.getNetworkCoinSync(userWalletId, network.id, network.derivationPath)
                        .getOrNull()
                }
                is FeePaidCurrency.Token -> {
                    currencyStatusOperations.getCurrencyStatusSync(userWalletId, feePaidCurrency.tokenId)
                        .getOrNull()
                }
                is FeePaidCurrency.SameCurrency,
                is FeePaidCurrency.FeeResource,
                -> cryptoCurrencyStatus
            }
        }
    }
}