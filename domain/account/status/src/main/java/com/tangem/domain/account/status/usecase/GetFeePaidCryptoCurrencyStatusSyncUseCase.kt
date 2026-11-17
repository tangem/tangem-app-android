package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository

class GetFeePaidCryptoCurrencyStatusSyncUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
) {

    /**
     * @param isHeldOutsidePortfolio whether [cryptoCurrencyStatus] belongs to an account outside the portfolio;
     * the fee currency is then taken from that account only, otherwise from the portfolio only
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isHeldOutsidePortfolio: Boolean = false,
    ): Either<TokenListError, CryptoCurrencyStatus?> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val network = cryptoCurrency.network
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, network)

        return either {
            when (feePaidCurrency) {
                is FeePaidCurrency.Coin -> {
                    if (isHeldOutsidePortfolio) {
                        findNonPortfolioStatus(userWalletId, network) { it.currency is CryptoCurrency.Coin }
                    } else {
                        singleAccountStatusListSupplier.getSyncOrNull(userWalletId)?.getCoinStatus(network)?.getOrNull()
                    }
                }
                is FeePaidCurrency.Token -> {
                    if (isHeldOutsidePortfolio) {
                        findNonPortfolioStatus(userWalletId, network) { it.currency.id == feePaidCurrency.tokenId }
                    } else {
                        singleAccountStatusListSupplier.getSyncOrNull(userWalletId).getCryptoCurrencyStatus(
                            currencyId = feePaidCurrency.tokenId,
                            network = network,
                        ).getOrNull()
                    }
                }
                is FeePaidCurrency.SameCurrency,
                is FeePaidCurrency.FeeResource,
                -> cryptoCurrencyStatus
            }
        }
    }

    private suspend fun findNonPortfolioStatus(
        userWalletId: UserWalletId,
        network: Network,
        predicate: (CryptoCurrencyStatus) -> Boolean,
    ): CryptoCurrencyStatus? {
        return nonPortfolioCurrenciesProvider.getStatuses(userWalletId = userWalletId, network = network)
            .firstOrNull(predicate)
    }
}