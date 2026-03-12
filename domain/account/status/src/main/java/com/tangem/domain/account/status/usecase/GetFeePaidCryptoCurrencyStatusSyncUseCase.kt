package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository

class GetFeePaidCryptoCurrencyStatusSyncUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
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
                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWalletId)

                    accountStatusList?.getCoinStatus(network)?.getOrNull()
                }
                is FeePaidCurrency.Token -> {
                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWalletId)

                    accountStatusList.getCryptoCurrencyStatus(
                        currencyId = feePaidCurrency.tokenId,
                        network = network,
                    ).getOrNull()
                }
                is FeePaidCurrency.SameCurrency,
                is FeePaidCurrency.FeeResource,
                -> cryptoCurrencyStatus
            }
        }
    }
}