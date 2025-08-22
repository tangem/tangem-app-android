package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.isStakingSupported
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.utils.CurrencyStatusProxyCreator
import kotlinx.coroutines.flow.*

/**
 * Base operations for working with currency status
 *
 * @property currenciesRepository repository for currencies
 *
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass", "LongParameterList")
abstract class BaseCurrencyStatusOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    private val multiYieldBalanceSupplier: MultiYieldBalanceSupplier,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val stakingIdFactory: StakingIdFactory,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    protected val currencyStatusProxyCreator = CurrencyStatusProxyCreator()

    abstract fun getCurrenciesStatuses(userWalletId: UserWalletId): LceFlow<TokenListError, List<CryptoCurrencyStatus>>

    protected abstract fun getQuotes(id: CryptoCurrency.RawID): Flow<Either<Error, Set<QuoteStatus>>>

    protected abstract suspend fun fetchComponents(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        currenciesIds: Set<CryptoCurrency.ID>,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit>

    suspend fun getCurrencyStatusFlow(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = {
                if (isSingleWalletWithTokens) {
                    getSingleCurrencyWalletWithCardTokensCurrency(userWalletId, currencyId)
                } else {
                    getMultiCurrencyWalletCurrency(userWalletId, currencyId)
                }
            },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(userWalletId = userWalletId, currency = currency)
    }

    suspend fun getCurrencyStatusFlow(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        includeQuotes: Boolean = true,
        subscribeOnYieldBalance: Boolean = true,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val rawCurrencyId = currency.id.rawCurrencyId

        val quoteFlow = if (includeQuotes && rawCurrencyId != null) {
            getQuotes(rawCurrencyId)
                .map { maybeQuotes ->
                    maybeQuotes.flatMap { quotes ->
                        quotes.singleOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }?.right()
                            ?: Error.EmptyQuotes.left()
                    }
                }
        } else {
            // don't use emptyFlow()
            flow { emit(Error.EmptyQuotes.left()) }
        }

        val statusFlow = getNetworkStatus(userWalletId = userWalletId, network = currency.network)

        val isStakingSupported = currency.network.toBlockchain().isStakingSupported

        val yieldBalanceFlow = if (isStakingSupported) {
            val stakingId = stakingIdFactory.create(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )
                .getOrNull()

            stakingId?.let {
                getYieldBalance(userWalletId = userWalletId, stakingId = it)
            }
        } else {
            null
        }

        return if (subscribeOnYieldBalance && yieldBalanceFlow != null) {
            combine(quoteFlow, statusFlow, yieldBalanceFlow) { maybeQuote, maybeNetworkStatus, maybeYieldBalance ->
                currencyStatusProxyCreator.createCurrencyStatus(
                    currency = currency,
                    maybeQuoteStatus = maybeQuote,
                    maybeNetworkStatus = maybeNetworkStatus,
                    maybeYieldBalance = maybeYieldBalance,
                )
            }
        } else {
            combine(quoteFlow, statusFlow) { maybeQuote, maybeNetworkStatus ->
                currencyStatusProxyCreator.createCurrencyStatus(
                    currency = currency,
                    maybeQuoteStatus = maybeQuote,
                    maybeNetworkStatus = maybeNetworkStatus,
                    maybeYieldBalance = null,
                )
            }
        }
    }

    suspend fun getNetworkCoinFlow(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        includeQuotes: Boolean = true,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getNetworkCoin(userWalletId, networkId, derivationPath) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(userWalletId, currency, includeQuotes)
    }

    suspend fun getNetworkCoinForSingleWalletWithTokenFlow(
        userWalletId: UserWalletId,
        networkId: Network.ID,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getNetworkCoinForSingleWalletWithToken(userWalletId, networkId) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(userWalletId, currency)
    }

    suspend fun getNetworkCoinSync(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Either<Error, CryptoCurrencyStatus> {
        val currency = recover(
            block = { getNetworkCoin(userWalletId, networkId, derivationPath) },
            recover = { return it.left() },
        )

        return getCurrencyStatusSync(userWalletId, currency.id)
    }

    suspend fun getCurrencyStatusSync(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean = false,
    ): Either<Error, CryptoCurrencyStatus> {
        return either {
            catch(
                block = {
                    val currency = if (isSingleWalletWithTokens) {
                        currenciesRepository.getSingleCurrencyWalletWithCardCurrency(userWalletId, cryptoCurrencyId)
                    } else {
                        getMultiCurrencyWalletCurrency(userWalletId, cryptoCurrencyId)
                    }

                    val quote = cryptoCurrencyId.rawCurrencyId?.let { rawId ->
                        singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawId))
                            .firstOrNull()
                    }
                        ?.right()
                        ?: Error.EmptyQuotes.left()

                    val networkStatuses = singleNetworkStatusSupplier(
                        params = SingleNetworkStatusProducer.Params(
                            userWalletId = userWalletId,
                            network = currency.network,
                        ),
                    )
                        .firstOrNull()
                        .right()

                    val yieldBalances = getYieldBalanceSync(userWalletId, currency)

                    return currencyStatusProxyCreator.createCurrencyStatus(
                        currency = currency,
                        maybeQuoteStatus = quote,
                        maybeNetworkStatus = networkStatuses,
                        maybeYieldBalance = yieldBalances,
                    )
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    suspend fun getNetworkCoinForSingleWalletWithTokenSync(
        userWalletId: UserWalletId,
        networkId: Network.ID,
    ): Either<Error, CryptoCurrencyStatus> = either {
        val currency = getNetworkCoinForSingleWalletWithToken(userWalletId, networkId)

        return getCurrencyStatusSync(userWalletId, currency.id)
    }

    suspend fun getPrimaryCurrencyStatusFlow(
        userWalletId: UserWalletId,
        includeQuotes: Boolean = true,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getPrimaryCurrency(userWalletId) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(
            userWalletId = userWalletId,
            currency = currency,
            includeQuotes = includeQuotes,
            subscribeOnYieldBalance = false,
        )
    }

    suspend fun getCurrenciesStatusesSync(userWalletId: UserWalletId): Either<Error, List<CryptoCurrencyStatus>> {
        return either {
            catch(
                block = {
                    val nonEmptyCurrencies = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                        multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                        )
                            ?.toNonEmptyListOrNull()
                    } else {
                        currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId).toNonEmptyListOrNull()
                    }
                        ?: return emptyList<CryptoCurrencyStatus>().right()

                    val (_, currenciesIds) = getIds(nonEmptyCurrencies)
                    val rawIds = currenciesIds.mapNotNull { it.rawCurrencyId }.toSet()

                    val quotes = quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = rawIds)?.right()

                    val networkStatuses = multiNetworkStatusSupplier(
                        params = MultiNetworkStatusProducer.Params(userWalletId = userWalletId),
                    )
                        .firstOrNull()
                        .orEmpty()
                        .right()

                    val yieldBalances = getYieldBalancesSync(userWalletId, nonEmptyCurrencies)

                    return currencyStatusProxyCreator.createCurrenciesStatuses(
                        currencies = nonEmptyCurrencies,
                        maybeQuotes = quotes,
                        maybeNetworkStatuses = networkStatuses,
                        maybeYieldBalances = yieldBalances,
                    )
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    suspend fun getPrimaryCurrencyStatusSync(userWalletId: UserWalletId): Either<Error, CryptoCurrencyStatus> = either {
        val currency = catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )

        val quotes = currency.id.rawCurrencyId?.let {
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(rawCurrencyId = it))
                .firstOrNull()
        }
            ?.right()
            ?: Error.EmptyQuotes.left()

        val networkStatus = singleNetworkStatusSupplier(
            params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = currency.network),
        )
            .firstOrNull()
            .right()

        val yieldBalances = getYieldBalanceSync(userWalletId, currency)

        return currencyStatusProxyCreator.createCurrencyStatus(
            currency = currency,
            maybeQuoteStatus = quotes,
            maybeNetworkStatus = networkStatus,
            maybeYieldBalance = yieldBalances,
        )
    }

    private suspend fun Raise<Error>.getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
    ): CryptoCurrency {
        return Either.catch {
            if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                    params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                )
                    ?.firstOrNull { it.id == currencyId }
                    ?: error("Unable to find currency with ID: $currencyId")
            } else {
                currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId = userWalletId, id = currencyId)
            }
        }
            .mapLeft(Error::DataError)
            .bind()
    }

    private suspend fun Raise<Error>.getSingleCurrencyWalletWithCardTokensCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
    ): CryptoCurrency {
        return Either.catch { currenciesRepository.getSingleCurrencyWalletWithCardCurrency(userWalletId, currencyId) }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private fun getYieldBalance(userWalletId: UserWalletId, stakingId: StakingID): EitherFlow<Error, YieldBalance> {
        return singleYieldBalanceSupplier(
            params = SingleYieldBalanceProducer.Params(
                userWalletId = userWalletId,
                stakingId = stakingId,
            ),
        )
            .map<YieldBalance, Either<Error, YieldBalance>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyYieldBalances.left()) }
    }

    private fun getNetworkStatus(userWalletId: UserWalletId, network: Network): EitherFlow<Error, NetworkStatus> {
        return singleNetworkStatusSupplier(
            params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = network),
        )
            .map<NetworkStatus, Either<Error, NetworkStatus>>(NetworkStatus::right)
            .distinctUntilChanged()
            .onEmpty { emit(Error.EmptyNetworksStatuses.left()) }
    }

    private suspend fun Raise<Error>.getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency {
        return Either.catch {
            if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                    params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                )
                    ?.filterIsInstance<CryptoCurrency.Coin>()
                    ?.firstOrNull { it.network.id == networkId }
                    ?: error("Unable to create network coin with ID: $networkId")
            } else {
                currenciesRepository.getNetworkCoin(userWalletId, networkId, derivationPath)
            }
        }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun Raise<Error>.getNetworkCoinForSingleWalletWithToken(
        userWalletId: UserWalletId,
        networkId: Network.ID,
    ): CryptoCurrency {
        return Either.catch {
            currenciesRepository.getSingleCurrencyWalletWithCardCurrencies(userWalletId)
                .find { it.network.id == networkId && it is CryptoCurrency.Coin }
                ?: raise(Error.DataError(IllegalStateException("Coin with network $networkId not found for this card")))
        }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun getYieldBalancesSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Either<Error.EmptyYieldBalances, List<YieldBalance>> = either {
        val stakingIds = cryptoCurrencies.mapNotNull { cryptoCurrency ->
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrency)
                .getOrNull()
        }

        ensure(stakingIds.isNotEmpty()) { Error.EmptyYieldBalances }

        val balances = multiYieldBalanceSupplier.getSyncOrNull(
            params = MultiYieldBalanceProducer.Params(userWalletId = userWalletId),
        )
            .orEmpty()
            .filter { it.stakingId in stakingIds }

        ensure(balances.isNotEmpty()) { Error.EmptyYieldBalances }

        balances
    }

    private suspend fun getYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<Error, YieldBalance> = either {
        val stakingId = stakingIdFactory.create(userWalletId, cryptoCurrency)
            .mapLeft {
                val exception = IllegalStateException("$it")
                Error.DataError(exception)
            }
            .bind()

        val yieldBalance = singleYieldBalanceSupplier.getSyncOrNull(
            params = SingleYieldBalanceProducer.Params(
                userWalletId = userWalletId,
                stakingId = stakingId,
            ),
        )

        ensureNotNull(yieldBalance) { Error.EmptyYieldBalances }
    }

    private suspend fun Raise<Error>.getPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    protected fun getIds(currencies: List<CryptoCurrency>): Pair<NonEmptySet<Network>, NonEmptySet<CryptoCurrency.ID>> {
        val currencyIdToNetworkId = currencies.associate { currency ->
            currency.id to currency.network
        }
        val currenciesIds = currencyIdToNetworkId.keys.toNonEmptySetOrNull()
        val networks = currencyIdToNetworkId.values.toNonEmptySetOrNull()

        requireNotNull(currenciesIds) { "Currencies IDs cannot be empty" }
        requireNotNull(networks) { "Networks IDs cannot be empty" }

        return networks to currenciesIds
    }
}