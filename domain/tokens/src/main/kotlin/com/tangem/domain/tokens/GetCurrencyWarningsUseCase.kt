package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
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
        currencyStatus: CryptoCurrencyStatus,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Set<CryptoCurrencyWarning>> {
        val currency = currencyStatus.currency
        return combine(
            getCoinRelatedWarnings(
                userWalletId = userWalletId,
                networkId = currency.network.id,
                currencyId = currency.id,
                derivationPath = derivationPath,
                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
                isSingleWalletWithTokens = isSingleWalletWithTokens,
            ),
            flowOf(walletManagersFacade.getRentInfo(userWalletId, currency.network)),
            flowOf(walletManagersFacade.getExistentialDeposit(userWalletId, currency.network)),
            flowOf(getNetworkUnavailableWarning(currencyStatus)),
            flowOf(getNetworkNoAccountWarning(currencyStatus)),
        ) { coinRelatedWarnings, maybeRentWarning, maybeEdWarning, maybeNetworkUnavailable, maybeNetworkNoAccount ->
            setOfNotNull(
                maybeRentWarning,
                maybeEdWarning?.let {
                    CryptoCurrencyWarning.ExistentialDeposit(
                        currencyName = currency.name,
                        edStringValueWithSymbol = "${it.toPlainString()} ${currency.symbol}",
                    )
                },
                *coinRelatedWarnings.toTypedArray(),
                maybeNetworkUnavailable,
                maybeNetworkNoAccount,
            )
        }.flowOn(dispatchers.io)
    }

    @Suppress("LongParameterList")
    private suspend fun getCoinRelatedWarnings(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        currencyId: CryptoCurrency.ID,
        contractAddress: String?,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<List<CryptoCurrencyWarning>> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )

        val currencyFlow = if (isSingleWalletWithTokens) {
            operations.getCurrencyStatusSingleWalletWithTokensFlow(currencyId)
        } else {
            operations.getCurrencyStatusFlow(currencyId, contractAddress, derivationPath)
        }
        val networkFlow = if (isSingleWalletWithTokens) {
            operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
        } else {
            operations.getNetworkCoinFlow(networkId, derivationPath)
        }
        return combine(
            currencyFlow.map { it.getOrNull() },
            networkFlow.map { it.getOrNull() },
        ) { tokenStatus, coinStatus ->
            when {
                tokenStatus != null && coinStatus != null -> {
                    buildList {
                        if (tokenStatus.value.hasCurrentNetworkTransactions) {
                            add(CryptoCurrencyWarning.HasPendingTransactions(coinStatus.currency.symbol))
                        }
                        if (!tokenStatus.value.amount.isZero() && coinStatus.value.amount.isZero()) {
                            add(
                                CryptoCurrencyWarning.BalanceNotEnoughForFee(
                                    tokenCurrency = tokenStatus.currency,
                                    coinCurrency = coinStatus.currency,
                                ),
                            )
                        }
                    }
                }
                else -> listOf(CryptoCurrencyWarning.SomeNetworksUnreachable)
            }
        }
    }

    private fun getNetworkUnavailableWarning(currencyStatus: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return (currencyStatus.value as? CryptoCurrencyStatus.Unreachable)?.let {
            CryptoCurrencyWarning.SomeNetworksUnreachable
        }
    }

    private fun getNetworkNoAccountWarning(currencyStatus: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return (currencyStatus.value as? CryptoCurrencyStatus.NoAccount)?.let {
            CryptoCurrencyWarning.SomeNetworksNoAccount(
                amountToCreateAccount = it.amountToCreateAccount,
                amountCurrency = currencyStatus.currency,
            )
        }
    }

    private fun BigDecimal?.isZero(): Boolean {
        return this?.signum() == 0
    }
}