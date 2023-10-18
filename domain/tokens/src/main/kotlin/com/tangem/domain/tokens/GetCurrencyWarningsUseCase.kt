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
            getFeeWarningFlow(
                userWalletId = userWalletId,
                networkId = currency.network.id,
                currencyId = currency.id,
                derivationPath = derivationPath,
                isSingleWalletWithTokens = isSingleWalletWithTokens,
            ),
            flowOf(walletManagersFacade.getRentInfo(userWalletId, currency.network)),
            flowOf(walletManagersFacade.getExistentialDeposit(userWalletId, currency.network)),
            flowOf(getNetworkUnavailableWarning(currencyStatus)),
            flowOf(getNetworkNoAccountWarning(currencyStatus)),
        ) { maybeFeeWarning, maybeRentWarning, maybeEdWarning, maybeNetworkUnavailable, maybeNetworkNoAccount ->
            setOfNotNull(
                maybeRentWarning,
                maybeEdWarning?.let {
                    CryptoCurrencyWarning.ExistentialDeposit(
                        currencyName = currency.name,
                        edStringValueWithSymbol = "${it.toPlainString()} ${currency.symbol}",
                    )
                },
                maybeFeeWarning,
                maybeNetworkUnavailable,
                maybeNetworkNoAccount,
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getFeeWarningFlow(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        currencyId: CryptoCurrency.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<CryptoCurrencyWarning?> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )

        val currencyFlow = if (isSingleWalletWithTokens) {
            operations.getCurrencyStatusSingleWalletWithTokensFlow(currencyId)
        } else {
            operations.getCurrencyStatusFlow(currencyId, derivationPath)
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