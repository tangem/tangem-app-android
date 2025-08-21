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
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.utils.extractAddress
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.extensions.isSingleItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList", "LargeClass")
class CachedCurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    quotesRepository: QuotesRepository,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    multiYieldBalanceSupplier: MultiYieldBalanceSupplier,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val stakingIdFactory: StakingIdFactory,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : BaseCurrencyStatusOperations(
    currenciesRepository = currenciesRepository,
    quotesRepository = quotesRepository,
    multiNetworkStatusSupplier = multiNetworkStatusSupplier,
    singleNetworkStatusSupplier = singleNetworkStatusSupplier,
    singleQuoteStatusSupplier = singleQuoteStatusSupplier,
    singleYieldBalanceSupplier = singleYieldBalanceSupplier,
    multiYieldBalanceSupplier = multiYieldBalanceSupplier,
    multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
    stakingIdFactory = stakingIdFactory,
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

    @Suppress("LongMethod")
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun transformToCurrenciesStatuses(
        userWalletId: UserWalletId,
        currenciesFlow: EitherFlow<TokenListError, List<CryptoCurrency>>,
    ): LceFlow<TokenListError, List<CryptoCurrencyStatus>> = lceFlow {
        val prevStatuses = MutableStateFlow(value = emptyList<CryptoCurrencyStatus>())

        val nonEmptyCurrencies = currenciesFlow.mapNotNull { it.getOrNull() }.firstOrNull()?.toNonEmptyListOrNull()

        if (!tokensFeatureToggles.isWalletBalanceFetcherEnabled && !isFetchingStarted(userWalletId) &&
            nonEmptyCurrencies != null
        ) {
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
                maybeQuotes: Either<TokenListError, Set<QuoteStatus>>,
                maybeNetworkStatuses: Either<TokenListError, Set<NetworkStatus>>,
                maybeYieldBalances: Either<TokenListError, List<YieldBalance>>,
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

            if (!tokensFeatureToggles.isWalletBalanceFetcherEnabled && !isFetchingStarted(userWalletId)) {
                launch {
                    setFetchStarted(userWalletId)

                    fetchComponents(userWalletId, networks, currenciesIds, currencies)
                }
                    .invokeOnCompletion { setFetchFinished(userWalletId) }
            }

            val networksStatusesUpdates = getNetworkStatusesUpdates(userWalletId, networks)

            combine(
                flow = getQuotes(currenciesIds),
                flow2 = networksStatusesUpdates,
                flow3 = networksStatusesUpdates.flatMapLatest {
                    val currenciesAddresses = it.getOrElse(default = { emptySet() })
                        .mapNotNull {
                            val currency = currencies.firstOrNull { currency -> currency.network == it.network }
                                ?: return@mapNotNull null

                            currency.id to extractAddress(it)
                        }
                        .toMap()

                    getYieldsBalancesUpdates(userWalletId, currenciesAddresses)
                },
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

                    multiQuoteStatusFetcher(
                        params = MultiQuoteStatusFetcher.Params(currenciesIds = rawCurrenciesIds, appCurrencyId = null),
                    )
                },
                async {
                    val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
                        stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
                    }

                    multiYieldBalanceFetcher(
                        params = MultiYieldBalanceFetcher.Params(
                            userWalletId = userWalletId,
                            stakingIds = stakingIds,
                        ),
                    )
                },
            )
        }
            .map { }
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<TokenListError, Set<QuoteStatus>>?,
        maybeNetworkStatuses: Either<TokenListError, Set<NetworkStatus>>?,
        maybeYieldBalances: Either<TokenListError, List<YieldBalance>>?,
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
                quoteStatus = quote,
                networkStatus = networkStatus,
                yieldBalance = yieldBalance,
                ignoreQuote = quotesRetrievingFailed,
            )

            currencyStatus
        }
    }

    private fun findYieldBalanceOrNull(
        yieldBalances: List<YieldBalance>?,
        currency: CryptoCurrency,
        networkStatus: NetworkStatus?,
    ): YieldBalance? {
        if (yieldBalances.isNullOrEmpty()) return null

        val supportedIntegration = StakingIntegrationID.create(currencyId = currency.id)?.value
        val address = extractAddress(networkStatus)

        return if (supportedIntegration != null && address != null) {
            val stakingId = StakingID(integrationId = supportedIntegration, address = address)

            yieldBalances.firstOrNull { it.stakingId == stakingId }
                ?: YieldBalance.Error(stakingId = stakingId)
        } else {
            null
        }
    }

    private fun getCurrencies(userWalletId: UserWalletId): EitherFlow<TokenListError, List<CryptoCurrency>> {
        return currenciesRepository.getWalletCurrenciesUpdates(userWalletId)
            .map<List<CryptoCurrency>, Either<TokenListError, List<CryptoCurrency>>> { it.right() }
            .catch { emit(TokenListError.DataError(it).left()) }
            .distinctUntilChanged()
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<TokenListError, Set<QuoteStatus>>> {
        return getQuotesUpdates(
            rawCurrencyIds = tokensIds.mapNotNullTo(
                destination = hashSetOf(),
                transform = CryptoCurrency.ID::rawCurrencyId,
            ),
        )
    }

    override fun getQuotes(id: CryptoCurrency.RawID): Flow<Either<Error, Set<QuoteStatus>>> {
        return singleQuoteStatusSupplier(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = id),
        )
            .map<QuoteStatus, Either<Error, Set<QuoteStatus>>> { setOf(it).right() }
            .distinctUntilChanged()
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
    private fun getQuotesUpdates(
        rawCurrencyIds: Set<CryptoCurrency.RawID>,
    ): EitherFlow<TokenListError, Set<QuoteStatus>> {
        return channelFlow {
            val state = MutableStateFlow(emptySet<QuoteStatus>())

            rawCurrencyIds.onEach {
                launch {
                    singleQuoteStatusSupplier(
                        params = SingleQuoteStatusProducer.Params(rawCurrencyId = it),
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
            .map<Set<QuoteStatus>, Either<TokenListError, Set<QuoteStatus>>> { it.right() }
            .distinctUntilChanged()
    }

    // temporary code because token list is built using networks list
    private fun getYieldsBalancesUpdates(
        userWalletId: UserWalletId,
        cryptoCurrencies: Map<CryptoCurrency.ID, String?>,
    ): EitherFlow<TokenListError, List<YieldBalance>> {
        return channelFlow {
            val state = MutableStateFlow(emptyList<YieldBalance>())

            val stakingIds = cryptoCurrencies.mapNotNullTo(hashSetOf()) {
                stakingIdFactory.create(currencyId = it.key, defaultAddress = it.value)
                    .getOrNull()
            }

            stakingIds.onEach { stakingId ->
                launch {
                    singleYieldBalanceSupplier(
                        params = SingleYieldBalanceProducer.Params(userWalletId = userWalletId, stakingId = stakingId),
                    )
                        .onEach { balance ->
                            state.update { loadedBalances ->
                                loadedBalances.addOrReplace(balance) { balance.stakingId == it.stakingId }
                            }
                        }
                        .launchIn(scope = this)
                }
            }

            state
                .onEach { send(it.right()) }
                .launchIn(scope = this)
        }
            .distinctUntilChanged()
    }

    private fun isFetchingStarted(userWalletId: UserWalletId): Boolean {
        return fetchingState.value[userWalletId]?.let { it.isStarted() || it.isFinished() } == true
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

        private val fetchingState = MutableStateFlow(value = emptyMap<UserWalletId, FetchingState>())
    }
}