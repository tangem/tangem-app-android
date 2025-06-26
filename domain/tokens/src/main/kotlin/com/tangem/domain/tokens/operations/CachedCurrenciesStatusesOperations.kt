package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.recover
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.lce.lceFlow
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.QuotesRepositoryV2
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.utils.extractAddress
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.extensions.isSingleItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList", "LargeClass")
class CachedCurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    quotesRepositoryV2: QuotesRepositoryV2,
    private val stakingRepository: StakingRepository,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val singleQuoteSupplier: SingleQuoteSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : BaseCurrenciesStatusesOperations,
    BaseCurrencyStatusOperations(
        currenciesRepository = currenciesRepository,
        quotesRepositoryV2 = quotesRepositoryV2,
        stakingRepository = stakingRepository,
        multiNetworkStatusSupplier = multiNetworkStatusSupplier,
        singleNetworkStatusSupplier = singleNetworkStatusSupplier,
        singleQuoteSupplier = singleQuoteSupplier,
        singleYieldBalanceSupplier = singleYieldBalanceSupplier,
        tokensFeatureToggles = tokensFeatureToggles,
    ) {

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

        val nonEmptyCurrencies = currenciesFlow.mapNotNull { it.getOrNull() }.firstOrNull()?.toNonEmptyListOrNull()

        if (!isFetchingStarted(userWalletId) && nonEmptyCurrencies != null) {
            launch {
                setFetchStarted(userWalletId)

                val (networks, currenciesIds) = getIds(nonEmptyCurrencies)
                fetchComponents(userWalletId, networks, currenciesIds, nonEmptyCurrencies)
            }
                .invokeOnCompletion { setFetchFinished(userWalletId) }
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

            if (!isFetchingStarted(userWalletId)) {
                launch {
                    setFetchStarted(userWalletId)

                    fetchComponents(userWalletId, networks, currenciesIds, currencies)
                }
                    .invokeOnCompletion { setFetchFinished(userWalletId) }
            }

            combine(
                flow = getQuotes(currenciesIds),
                flow2 = getNetworkStatusesUpdates(userWalletId, networks),
                flow3 = getYieldBalances(userWalletId, currencies),
                flow4 = fetchingState.map {
                    val state = it[userWalletId] ?: return@map false

                    !state.isFinished()
                },
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
    ): Either<Throwable, Unit> = either {
        coroutineScope {
            awaitAll(
                async {
                    if (networks.isSingleItem()) {
                        singleNetworkStatusFetcher(
                            params = SingleNetworkStatusFetcher.Params(
                                userWalletId = userWalletId,
                                network = networks.first(),
                            ),
                        )
                    } else {
                        multiNetworkStatusFetcher(
                            params = MultiNetworkStatusFetcher.Params(
                                userWalletId = userWalletId,
                                networks = networks,
                            ),
                        )
                    }
                },
                async {
                    val rawCurrenciesIds = currenciesIds.mapNotNullTo(mutableSetOf()) { it.rawCurrencyId }

                    multiQuoteFetcher(
                        params = MultiQuoteFetcher.Params(currenciesIds = rawCurrenciesIds, appCurrencyId = null),
                    )
                },
                async {
                    if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
                        multiYieldBalanceFetcher(
                            params = MultiYieldBalanceFetcher.Params(
                                userWalletId = userWalletId,
                                currencyIdWithNetworkMap = currencies.associateTo(hashMapOf()) { it.id to it.network },
                            ),
                        )
                    } else {
                        stakingRepository.fetchMultiYieldBalance(userWalletId, currencies)
                    }
                },
            )
        }
            .map { }
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
        return getQuotesUpdates(
            rawCurrencyIds = tokensIds.mapNotNullTo(
                destination = hashSetOf(),
                transform = CryptoCurrency.ID::rawCurrencyId,
            ),
        )
    }

    override fun getQuotes(id: CryptoCurrency.RawID): Flow<Either<Error, Set<Quote>>> {
        return singleQuoteSupplier(
            params = SingleQuoteProducer.Params(rawCurrencyId = id),
        )
            .map<Quote, Either<Error, Set<Quote>>> { setOf(it).right() }
            .distinctUntilChanged()
    }

    private fun getYieldBalances(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): EitherFlow<TokenListError, YieldBalanceList> {
        return if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            getYieldsBalancesUpdates(userWalletId, cryptoCurrencies)
        } else {
            stakingRepository.getMultiYieldBalanceUpdates(userWalletId, cryptoCurrencies)
                .map<YieldBalanceList, Either<TokenListError, YieldBalanceList>> { it.right() }
                .retryWhen { cause, _ ->
                    emit(TokenListError.DataError(cause).left())
                    // adding delay before retry to avoid spam when flow restarted
                    delay(RETRY_DELAY)
                    true
                }
                .distinctUntilChanged()
        }
    }

    // temporary code because token list is built using networks list
    @OptIn(FlowPreview::class)
    private fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: NonEmptySet<Network>,
    ): EitherFlow<TokenListError, Set<NetworkStatus>> {
        return channelFlow {
            val state = MutableStateFlow(emptySet<NetworkStatus>())

            networks.onEach {
                launch {
                    singleNetworkStatusSupplier(
                        params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = it),
                    )
                        .onEach { status ->
                            state.update { loadedStatuses ->
                                loadedStatuses.addOrReplace(status) { it.network == status.network }
                            }
                        }
                        .launchIn(scope = this)
                }
            }

            state
                .onEach(::send)
                .launchIn(scope = this)
        }
            .debounce(timeoutMillis = 500)
            .map<Set<NetworkStatus>, Either<TokenListError, Set<NetworkStatus>>> { it.right() }
            .distinctUntilChanged()
    }

    // temporary code because token list is built using networks list
    private fun getQuotesUpdates(rawCurrencyIds: Set<CryptoCurrency.RawID>): EitherFlow<TokenListError, Set<Quote>> {
        return channelFlow {
            val state = MutableStateFlow(emptySet<Quote>())

            rawCurrencyIds.onEach {
                launch {
                    singleQuoteSupplier(
                        params = SingleQuoteProducer.Params(rawCurrencyId = it),
                    )
                        .onEach { quote ->
                            state.update { loadedStatuses ->
                                loadedStatuses.addOrReplace(quote) { it.rawCurrencyId == quote.rawCurrencyId }
                            }
                        }
                        .launchIn(scope = this)
                }
            }

            state
                .onEach(::send)
                .launchIn(scope = this)
        }
            .map<Set<Quote>, Either<TokenListError, Set<Quote>>> { it.right() }
            .distinctUntilChanged()
    }

    // temporary code because token list is built using networks list
    private fun getYieldsBalancesUpdates(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): EitherFlow<TokenListError, YieldBalanceList> {
        return channelFlow {
            val state = MutableStateFlow(emptyList<YieldBalance>())

            cryptoCurrencies.onEach {
                launch {
                    singleYieldBalanceSupplier(
                        params = SingleYieldBalanceProducer.Params(
                            userWalletId = userWalletId,
                            currencyId = it.id,
                            network = it.network,
                        ),
                    )
                        .onEach { balance ->
                            state.update { loadedBalances ->
                                loadedBalances.addOrReplace(balance) {
                                    it.integrationId == balance.integrationId && it.address == balance.address
                                }
                            }
                        }
                        .launchIn(scope = this)
                }
            }

            state
                .onEach(::send)
                .launchIn(scope = this)
        }
            .map<List<YieldBalance>, Either<TokenListError, YieldBalanceList>> { balances ->
                val yieldBalanceList = if (balances.isEmpty()) {
                    YieldBalanceList.Empty
                } else {
                    YieldBalanceList.Data(balances)
                }

                yieldBalanceList.right()
            }
            .distinctUntilChanged()
    }

    private fun isFetchingStarted(userWalletId: UserWalletId): Boolean {
        return fetchingState.value[userWalletId]?.let { it.isStarted() || it.isFinished() } ?: false
    }

    private fun setFetchStarted(userWalletId: UserWalletId) {
        fetchingState.update {
            it.toMutableMap().apply {
                put(key = userWalletId, value = FetchingState.STARTED)
            }
        }
    }

    private fun setFetchFinished(userWalletId: UserWalletId) {
        fetchingState.update {
            it.toMutableMap().apply {
                put(key = userWalletId, value = FetchingState.FINISHED)
            }
        }
    }

    enum class FetchingState {
        STARTED, FINISHED;

        fun isStarted() = this == STARTED
        fun isFinished() = this == FINISHED
    }

    companion object {
        internal const val RETRY_DELAY = 2000L

        private val fetchingState = MutableStateFlow(value = emptyMap<UserWalletId, FetchingState>())
    }
}
