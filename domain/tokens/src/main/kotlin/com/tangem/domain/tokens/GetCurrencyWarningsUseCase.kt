package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.settings.ShouldShowSwapPromoTokenUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

@Suppress("LongParameterList")
class GetCurrencyWarningsUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val swapRepository: SwapRepository,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val showSwapPromoTokenUseCase: ShouldShowSwapPromoTokenUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Set<CryptoCurrencyWarning>> {
        val currency = currencyStatus.currency
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )
        return combine(
            getCoinRelatedWarnings(
                operations = operations,
                networkId = currency.network.id,
                currencyId = currency.id,
                derivationPath = derivationPath,
                isSingleWalletWithTokens = isSingleWalletWithTokens,
            ),
            operations.getCurrenciesStatusesFlow().conflate(),
            flowOf(walletManagersFacade.getRentInfo(userWalletId, currency.network)),
            flowOf(walletManagersFacade.getExistentialDeposit(userWalletId, currency.network)),
            showSwapPromoTokenUseCase().conflate(),
        ) { coinRelatedWarnings, cryptoStatuses, maybeRentWarning, maybeEdWarning, shouldShowSwapPromo ->
            setOfNotNull(
                getSwapPromoNotificationWarning(
                    shouldShowSwapPromo = shouldShowSwapPromo,
                    userWalletId = userWalletId,
                    currencyStatus = currencyStatus,
                    cryptoStatuses = cryptoStatuses,
                ),
                maybeRentWarning,
                maybeEdWarning?.let {
                    CryptoCurrencyWarning.ExistentialDeposit(
                        currencyName = currency.name,
                        edStringValueWithSymbol = "${it.toPlainString()} ${currency.symbol}",
                    )
                },
                *coinRelatedWarnings.toTypedArray(),
                getNetworkUnavailableWarning(currencyStatus),
                getNetworkNoAccountWarning(currencyStatus),
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getSwapPromoNotificationWarning(
        shouldShowSwapPromo: Boolean,
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        cryptoStatuses: Either<CurrenciesStatusesOperations.Error, List<CryptoCurrencyStatus>>,
    ): CryptoCurrencyWarning? {
        val currency = currencyStatus.currency
        return if (shouldShowSwapPromo && marketCryptoCurrencyRepository.isExchangeable(userWalletId, currency)) {
            cryptoStatuses.fold(
                ifLeft = { null },
                ifRight = { cryptoCurrencyStatuses ->
                    val pairs = runCatching(dispatchers.io) {
                        swapRepository.getPairs(
                            LeastTokenInfo(
                                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
                                network = currency.network.backendId,
                            ),
                            cryptoCurrencyStatuses.map { it.currency },
                        )
                    }.getOrNull()?.pairs ?: emptyList()

                    val filteredCurrencies = cryptoCurrencyStatuses.filterNot {
                        it.currency.id == currency.id
                    }
                    val currencyPairs = pairs.filter {
                        it.from.network == currency.network.backendId ||
                            it.to.network == currency.network.backendId
                    }
                    val isExchangeable = currencyPairs.any { pair ->
                        val availablePair = if (currencyStatus.value.amount.isNullOrZero()) {
                            filteredCurrencies.filterNot { it.value.amount.isNullOrZero() }
                        } else {
                            filteredCurrencies
                        }
                        availablePair
                            .any {
                                (
                                    it.currency.network.backendId == pair.to.network ||
                                        it.currency.network.backendId == pair.from.network
                                    ) &&
                                    !it.value.amount.isNullOrZero()
                            }
                    }
                    if (isExchangeable) {
                        CryptoCurrencyWarning.SwapPromo
                    } else {
                        null
                    }
                },
            )
        } else {
            null
        }
    }

    @Suppress("LongParameterList")
    private suspend fun getCoinRelatedWarnings(
        operations: CurrenciesStatusesOperations,
        networkId: Network.ID,
        currencyId: CryptoCurrency.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<List<CryptoCurrencyWarning>> {
        val currencyFlow = if (isSingleWalletWithTokens) {
            operations.getCurrencyStatusSingleWalletWithTokensFlow(currencyId)
        } else {
            operations.getCurrencyStatusFlow(currencyId)
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
                        if (currenciesRepository.hasPendingTransactions(tokenStatus, coinStatus)) {
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