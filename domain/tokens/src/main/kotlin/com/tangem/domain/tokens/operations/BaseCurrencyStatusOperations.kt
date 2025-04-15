package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.recover
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.utils.CurrencyStatusProxyCreator
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

/**
 * Base operations for working with currency status
 *
 * @property currenciesRepository repository for currencies
 * @property quotesRepository     repository for quotes
 * @property networksRepository   repository for networks
 * @property stakingRepository    repository for staking
 *
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass", "LongParameterList")
abstract class BaseCurrencyStatusOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteSupplier: SingleQuoteSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    protected val currencyStatusProxyCreator = CurrencyStatusProxyCreator(stakingRepository)

    protected abstract fun getQuotes(id: CryptoCurrency.RawID): Flow<Either<Error, Set<Quote>>>

    protected abstract fun getNetworksStatuses(
        userWalletId: UserWalletId,
        network: Network,
    ): EitherFlow<Error, Set<NetworkStatus>>

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

        val statusFlow = getNetworksStatuses(userWalletId = userWalletId, network = currency.network)
            .map { maybeStatuses ->
                maybeStatuses.flatMap { statuses ->
                    statuses.singleOrNull { it.network == currency.network }?.right()
                        ?: Error.EmptyNetworksStatuses.left()
                }
            }

        val yieldBalanceFlow = getYieldBalance(userWalletId = userWalletId, cryptoCurrency = currency)

        return combine(quoteFlow, statusFlow, yieldBalanceFlow) { maybeQuote, maybeNetworkStatus, maybeYieldBalance ->
            currencyStatusProxyCreator.createCurrencyStatus(
                currency = currency,
                maybeQuote = maybeQuote,
                maybeNetworkStatus = maybeNetworkStatus,
                maybeYieldBalance = maybeYieldBalance,
            )
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
                        if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
                            singleQuoteSupplier(params = SingleQuoteProducer.Params(rawCurrencyId = rawId))
                                .firstOrNull()
                        } else {
                            quotesRepository.getQuoteSync(rawId)
                        }
                    }
                        ?.right()
                        ?: Error.EmptyQuotes.left()

                    val networkStatuses = if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
                        singleNetworkStatusSupplier(
                            params = SingleNetworkStatusProducer.Params(
                                userWalletId = userWalletId,
                                network = currency.network,
                            ),
                        )
                            .firstOrNull()
                            .right()
                    } else {
                        networksRepository.getNetworkStatusesSync(
                            userWalletId = userWalletId,
                            networks = setOf(currency.network),
                            refresh = false,
                        )
                            .firstOrNull { it.network == currency.network }
                            .right()
                    }

                    val yieldBalances = getYieldBalanceSync(userWalletId, currency)

                    return currencyStatusProxyCreator.createCurrencyStatus(
                        currency = currency,
                        maybeQuote = quote,
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

        return getCurrencyStatusFlow(userWalletId, currency, includeQuotes)
    }

    suspend fun getCurrenciesStatusesSync(userWalletId: UserWalletId): Either<Error, List<CryptoCurrencyStatus>> {
        return either {
            catch(
                block = {
                    val nonEmptyCurrencies =
                        currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId).toNonEmptyListOrNull()
                            ?: return emptyList<CryptoCurrencyStatus>().right()
                    val (networks, currenciesIds) = getIds(nonEmptyCurrencies)
                    val rawIds = currenciesIds.mapNotNull { it.rawCurrencyId }.toSet()
                    val quotes = quotesRepository.getQuotesSync(rawIds, false).right()

                    val networkStatuses = if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
                        multiNetworkStatusSupplier(
                            params = MultiNetworkStatusProducer.Params(userWalletId = userWalletId),
                        )
                            .firstOrNull()
                            .orEmpty()
                            .right()
                    } else {
                        networksRepository.getNetworkStatusesSync(userWalletId, networks, false).right()
                    }
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

        val quotes = if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            currency.id.rawCurrencyId?.let {
                singleQuoteSupplier(params = SingleQuoteProducer.Params(rawCurrencyId = it))
                    .firstOrNull()
            }
                ?.right()
                ?: Error.EmptyQuotes.left()
        } else {
            catch(
                block = {
                    currency.id.rawCurrencyId?.let { quotesRepository.getQuoteSync(it) }
                        ?.right() ?: Error.EmptyQuotes.left()
                },
                catch = { Error.DataError(it).left() },
            )
        }

        val networkStatus = if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
            singleNetworkStatusSupplier(
                params = SingleNetworkStatusProducer.Params(
                    userWalletId = userWalletId,
                    network = currency.network,
                ),
            )
                .firstOrNull()
                .right()
        } else {
            catch(
                block = {
                    networksRepository.getNetworkStatusesSync(userWalletId, setOf(currency.network))
                        .firstOrNull { it.network == currency.network }
                        .right()
                },
                catch = { Error.DataError(it).left() },
            )
        }

        val yieldBalances = getYieldBalanceSync(userWalletId, currency)

        return currencyStatusProxyCreator.createCurrencyStatus(
            currency = currency,
            maybeQuote = quotes,
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
        return stakingRepository.getSingleYieldBalanceFlow(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        ).map<YieldBalance, Either<Error, YieldBalance>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyYieldBalances.left()) }
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
                stakingRepository.getMultiYieldBalanceSync(
                    userWalletId,
                    cryptoCurrencies,
                ).right()
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
                stakingRepository.getSingleYieldBalanceSync(
                    userWalletId,
                    cryptoCurrency,
                ).right()
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