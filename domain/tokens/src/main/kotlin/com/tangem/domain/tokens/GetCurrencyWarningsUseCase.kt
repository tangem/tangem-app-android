package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

class GetCurrencyWarningsUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        derivationPath: Network.DerivationPath,
    ): Flow<Set<CryptoCurrencyWarning>> {
        return combine(
            getFeeWarningFlow(
                userWalletId = userWalletId,
                networkId = currency.network.id,
                currencyId = currency.id,
                derivationPath = derivationPath,
            ),
            flowOf(walletManagersFacade.getRentInfo(userWalletId, currency.network)),
            flowOf(walletManagersFacade.getExistentialDeposit(userWalletId, currency.network)),
        ) { maybeFeeWarning, maybeRentWarning, maybeEdWarning ->
            setOfNotNull(
                maybeRentWarning,
                maybeEdWarning?.let {
                    CryptoCurrencyWarning.ExistentialDeposit(
                        currencyName = currency.name,
                        edStringValueWithSymbol = "${it.toPlainString()} ${currency.symbol}",
                    )
                },
                maybeFeeWarning,
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getFeeWarningFlow(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        currencyId: CryptoCurrency.ID,
        derivationPath: Network.DerivationPath,
    ): Flow<CryptoCurrencyWarning?> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )

        return combine(
            operations.getCurrencyStatusFlow(currencyId, derivationPath).map { it.getOrNull() },
            operations.getNetworkCoinFlow(networkId, derivationPath).map { it.getOrNull() },
        ) { tokenStatus, coinStatus ->
            when {
                tokenStatus != null && coinStatus != null -> {
                    if (!tokenStatus.value.amount.isZero() && coinStatus.value.amount.isZero()) {
                        CryptoCurrencyWarning.BalanceNotEnoughForFee(
                            currency = tokenStatus.currency,
                            blockchainFullName = coinStatus.currency.name,
                            blockchainSymbol = coinStatus.currency.symbol,
                        )
                    } else {
                        null
                    }
                }
                else -> CryptoCurrencyWarning.SomeNetworksUnreachable
            }
        }
    }

    private fun BigDecimal?.isZero(): Boolean {
        return this?.signum() == 0
    }
}