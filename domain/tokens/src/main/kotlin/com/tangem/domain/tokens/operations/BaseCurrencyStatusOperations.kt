package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.recover
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.utils.CurrencyStatusProxyCreator
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

/**
 * Base operations for working with currency status
 *
 * @property currenciesRepository repository for currencies
 * @property stakingRepository    repository for staking
 *
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass", "LongParameterList")
abstract class BaseCurrencyStatusOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val stakingRepository: StakingRepository,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    protected val currencyStatusProxyCreator = CurrencyStatusProxyCreator(stakingRepository)

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

    fun getCurrencyStatusFlow(
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

        val yieldBalanceFlow = getYieldBalance(userWalletId = userWalletId, cryptoCurrency = currency)

        return if (subscribeOnYieldBalance) {
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
                        currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, cryptoCurrencyId)
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
            // If toggle is off, then subscribe on yield balance. If toggle is on, then don't
            subscribeOnYieldBalance = !tokensFeatureToggles.isStakingLoadingRefactoringEnabled,
        )
    }

    suspend fun getCurrenciesStatusesSync(userWalletId: UserWalletId): Either<Error, List<CryptoCurrencyStatus>> {
        return either {
            catch(
                block = {
                    val nonEmptyCurrencies =
                        currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId).toNonEmptyListOrNull()
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
            currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId = userWalletId, id = currencyId)
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

    private fun getYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): EitherFlow<Error, YieldBalance> {
        return if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            singleYieldBalanceSupplier(
                params = SingleYieldBalanceProducer.Params(
                    userWalletId = userWalletId,
                    currencyId = cryptoCurrency.id,
                    network = cryptoCurrency.network,
                ),
            )
        } else {
            stakingRepository.getSingleYieldBalanceFlow(userWalletId = userWalletId, cryptoCurrency = cryptoCurrency)
        }
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
        return Either.catch { currenciesRepository.getNetworkCoin(userWalletId, networkId, derivationPath) }
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
    ): Either<Error.EmptyYieldBalances, YieldBalanceList> {
        return catch(
            block = {
                if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
                    stakingRepository.getMultiYieldBalanceSync(
                        userWalletId = userWalletId,
                        cryptoCurrencies = cryptoCurrencies,
                    )
                } else {
                    stakingRepository.getMultiYieldBalanceSyncLegacy(
                        userWalletId = userWalletId,
                        cryptoCurrencies = cryptoCurrencies,
                    )
                }
                    .right()
            },
            catch = {
                Error.EmptyYieldBalances.left()
            },
        )
    }

    private suspend fun getYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<Error.EmptyYieldBalances, YieldBalance> {
        return catch(
            block = {
                if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
                    stakingRepository.getSingleYieldBalanceSync(userWalletId, cryptoCurrency)
                } else {
                    stakingRepository.getSingleYieldBalanceSyncLegacy(userWalletId, cryptoCurrency)
                }
                    .right()
            },
            catch = {
                Error.EmptyYieldBalances.left()
            },
        )
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