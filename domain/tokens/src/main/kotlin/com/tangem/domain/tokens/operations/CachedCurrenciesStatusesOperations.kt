package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.recover
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.lce.lceFlow
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.utils.extractAddress
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CachedCurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    @Suppress("UnusedPrivateMember") private val tokensFeatureToggles: TokensFeatureToggles,
) : BaseCurrenciesStatusesOperations,
    BaseCurrencyStatusOperations(currenciesRepository, quotesRepository, networksRepository, stakingRepository) {

    override fun getCurrenciesStatuses(
        userWalletId: UserWalletId,
    ): LceFlow<TokenListError, List<CryptoCurrencyStatus>> {
        return transformToCurrenciesStatuses(
            userWalletId = userWalletId,
            currenciesFlow = getCurrencies(userWalletId),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun transformToCurrenciesStatuses(
        userWalletId: UserWalletId,
        currenciesFlow: EitherFlow<TokenListError, List<CryptoCurrency>>,
    ): LceFlow<TokenListError, List<CryptoCurrencyStatus>> = lceFlow {
        val prevStatuses = MutableStateFlow(value = emptyList<CryptoCurrencyStatus>())

        val isUpdating = MutableStateFlow(value = true)
        val isFetchingStarted = MutableStateFlow(value = false)

        val nonEmptyCurrencies = currenciesFlow.mapNotNull { it.getOrNull() }.firstOrNull()?.toNonEmptyListOrNull()

        if (nonEmptyCurrencies != null) {
            launch {
                isFetchingStarted.value = true

                val (networks, currenciesIds) = getIds(nonEmptyCurrencies)
                fetchComponents(userWalletId, networks, currenciesIds, nonEmptyCurrencies)
            }
                .invokeOnCompletion { isUpdating.value = false }
        }

        currenciesFlow.flatMapLatest { maybeCurrencies ->
            val currencies = maybeCurrencies
                .getOrElse { return@flatMapLatest flowOf(it.lceError()) }
                .toNonEmptyListOrNull()

            if (currencies.isNullOrEmpty()) {
                prevStatuses.value = emptyList()
                return@flatMapLatest flowOf(TokenListError.EmptyTokens.lceError())
            }

            // This is only 'true' when the flow here is empty, such as during initial loading
            if (isLoading.get()) {
                val loadingCurrencies = createCurrenciesStatuses(
                    currencies = currencies,
                    maybeNetworkStatuses = null,
                    maybeQuotes = null,
                    maybeYieldBalances = null,
                    isUpdating = true,
                )

                loadingCurrencies.getOrNull()?.let { prevStatuses.value = it }

                send(loadingCurrencies)
            }

            val (networks, currenciesIds) = getIds(currencies)

            fun createCurrenciesStatuses(
                maybeQuotes: Either<TokenListError, Set<Quote>>?,
                maybeNetworkStatuses: Either<TokenListError, Set<NetworkStatus>>?,
                maybeYieldBalances: Either<TokenListError, YieldBalanceList>?,
                isUpdating: Boolean,
            ) = createCurrenciesStatuses(
                currencies = currencies,
                maybeQuotes = maybeQuotes,
                maybeNetworkStatuses = maybeNetworkStatuses,
                maybeYieldBalances = maybeYieldBalances,
                isUpdating = isUpdating,
            )

            // removing token
            val prevStatusesValue = prevStatuses.value
            if (prevStatusesValue.size - currencies.size == 1) {
                val removed = prevStatusesValue.map { it.currency } - currencies

                return@flatMapLatest flowOf(
                    prevStatusesValue.filter { it.currency !in removed }.lceContent(),
                )
            }

            if (!isFetchingStarted.value) {
                launch {
                    isFetchingStarted.value = true

                    fetchComponents(userWalletId, networks, currenciesIds, currencies)
                }
                    .invokeOnCompletion { isUpdating.value = false }
            }

            combine(
                flow = getQuotes(currenciesIds),
                flow2 = getNetworksStatuses(userWalletId, networks),
                flow3 = getYieldBalances(userWalletId, currencies),
                flow4 = isUpdating,
                transform = ::createCurrenciesStatuses,
            )
                .distinctUntilChanged()
        }
            .onEach { statusesLce ->
                statusesLce.getOrNull()?.let { prevStatuses.value = it }

                send(statusesLce)
            }
            .launchIn(scope = this)
    }

    override suspend fun fetchComponents(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        currenciesIds: Set<CryptoCurrency.ID>,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        return coroutineScope {
            Either.catch {
                awaitAll(
                    async { networksRepository.fetchNetworkStatuses(userWalletId, networks) },
                    async {
                        val rawCurrenciesIds = currenciesIds.mapNotNullTo(mutableSetOf()) { it.rawCurrencyId }
                        quotesRepository.fetchQuotes(rawCurrenciesIds)
                    },
                    async { stakingRepository.fetchMultiYieldBalance(userWalletId, currencies) },
                )
            }
                .map { }
        }
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<TokenListError, Set<Quote>>?,
        maybeNetworkStatuses: Either<TokenListError, Set<NetworkStatus>>?,
        maybeYieldBalances: Either<TokenListError, YieldBalanceList>?,
        isUpdating: Boolean,
    ): Lce<TokenListError, List<CryptoCurrencyStatus>> = lce {
        isLoading.set(isUpdating)

        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bindEither()?.toNonEmptySetOrNull()
        val yieldBalances = maybeYieldBalances?.bindEither()
        val quotes = recover({ maybeQuotes?.bind()?.toNonEmptySetOrNull() }) {
            null
        }

        if (quotes == null) {
            quotesRetrievingFailed = true
        }

        currencies.map { currency ->
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }
            val yieldBalance = findYieldBalanceOrNull(yieldBalances, currency, networkStatus)

            val currencyStatus = currencyStatusProxyCreator.createCurrencyStatus(
                currency = currency,
                quote = quote,
                networkStatus = networkStatus,
                yieldBalance = yieldBalance,
                ignoreQuote = quotesRetrievingFailed,
            )

            currencyStatus
        }
    }

    private fun findYieldBalanceOrNull(
        yieldBalances: YieldBalanceList?,
        currency: CryptoCurrency,
        networkStatus: NetworkStatus?,
    ): YieldBalance? {
        if (yieldBalances !is YieldBalanceList.Data) return null

        val supportedIntegration = stakingRepository.getSupportedIntegrationId(currency.id)

        if (supportedIntegration.isNullOrBlank()) return null

        return yieldBalances.getBalance(
            address = extractAddress(networkStatus),
            integrationId = supportedIntegration,
        )
    }

    private fun getCurrencies(userWalletId: UserWalletId): EitherFlow<TokenListError, List<CryptoCurrency>> {
        return currenciesRepository.getWalletCurrenciesUpdates(userWalletId)
            .map<List<CryptoCurrency>, Either<TokenListError, List<CryptoCurrency>>> { it.right() }
            .catch { emit(TokenListError.DataError(it).left()) }
            .distinctUntilChanged()
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<TokenListError, Set<Quote>>> {
        return quotesRepository.getQuotesUpdates(tokensIds.mapNotNull { it.rawCurrencyId }.toSet())
            .map<Set<Quote>, Either<TokenListError, Set<Quote>>> { it.right() }
            .retryWhen { cause, _ ->
                emit(TokenListError.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
    }

    override fun getQuotes(id: CryptoCurrency.RawID): Flow<Either<Error, Set<Quote>>> {
        return quotesRepository.getQuotesUpdates(setOf(id))
            .map<Set<Quote>, Either<Error, Set<Quote>>> { it.right() }
            .retryWhen { cause, _ ->
                emit(Error.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
    }

    override fun getNetworksStatuses(
        userWalletId: UserWalletId,
        network: Network,
    ): EitherFlow<Error, Set<NetworkStatus>> {
        return networksRepository.getNetworkStatusesUpdates(userWalletId, setOf(network))
            .map<Set<NetworkStatus>, Either<Error, Set<NetworkStatus>>> { it.right() }
            .retryWhen { cause, _ ->
                emit(Error.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
            .onEmpty { emit(Error.EmptyNetworksStatuses.left()) }
    }

    private fun getNetworksStatuses(
        userWalletId: UserWalletId,
        networks: NonEmptySet<Network>,
    ): EitherFlow<TokenListError, Set<NetworkStatus>> {
        return networksRepository.getNetworkStatusesUpdates(userWalletId, networks)
            .map<Set<NetworkStatus>, Either<TokenListError, Set<NetworkStatus>>> { it.right() }
            .retryWhen { cause, _ ->
                emit(TokenListError.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
    }

    private fun getYieldBalances(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): EitherFlow<TokenListError, YieldBalanceList> {
        return stakingRepository.getMultiYieldBalanceUpdates(userWalletId, cryptoCurrencies)
            .map<YieldBalanceList, Either<TokenListError, YieldBalanceList>> { it.right() }
            .retryWhen { cause, _ ->
                emit(TokenListError.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
    }

    companion object {
        internal const val RETRY_DELAY = 2000L
    }
}