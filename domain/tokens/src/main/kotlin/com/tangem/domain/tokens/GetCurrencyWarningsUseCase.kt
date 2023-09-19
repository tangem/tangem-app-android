package com.tangem.domain.tokens

import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.models.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
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

    suspend operator fun invoke(userWallet: UserWallet, currency: CryptoCurrency): Flow<Set<CryptoCurrencyWarning>> {
        return combine(
            getFeeWarningFlow(
                userWalletId = userWallet.walletId,
                networkId = currency.network.id,
                currencyId = currency.id,
            ),
            flowOf(walletManagersFacade.getRentInfo(userWallet.walletId, currency.network)),
            flowOf(walletManagersFacade.getExistentialDeposit(userWallet.walletId, currency.network)),
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
    ): Flow<CryptoCurrencyWarning?> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )

        return combine(
            operations.getCurrencyStatusFlow(currencyId).map { it.getOrNull() },
            operations.getNetworkCoinFlow(networkId).map { it.getOrNull() },
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
        return this?.compareTo(BigDecimal.ZERO) == 0
    }
}