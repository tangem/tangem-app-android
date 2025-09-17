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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.getAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "LargeClass")
class CachedCurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    quotesRepository: QuotesRepository,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    multiYieldBalanceSupplier: MultiYieldBalanceSupplier,
    multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val stakingIdFactory: StakingIdFactory,
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

            val networksStatusesUpdates = getNetworkStatusesUpdates(userWalletId, networks)

            combine(
                flow = getQuotes(currenciesIds),
                flow2 = networksStatusesUpdates,
                flow3 = networksStatusesUpdates.flatMapLatest { maybeNetworksStatuses ->
                    val networksStatuses = maybeNetworksStatuses.getOrNull()

                    val currenciesAddresses = if (networksStatuses == null) {
                        emptyMap()
                    } else {
                        currencies.associate { currency ->
                            val networkStatus = networksStatuses.firstOrNull { it.network == currency.network }

                            currency.id to networkStatus.getAddress()
                        }
                    }

                    getYieldsBalancesUpdates(userWalletId, currenciesAddresses)
                },
                flow4 = flowOf(value = false),
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
        val address = networkStatus.getAddress()

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

            val stakingIds = cryptoCurrencies.mapNotNullTo(hashSetOf()) { currencyWithAddress ->
                stakingIdFactory.create(
                    currencyId = currencyWithAddress.key,
                    defaultAddress = currencyWithAddress.value,
                )
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
}