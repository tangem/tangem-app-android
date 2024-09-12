package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

// FIXME: Refactor - https://tangem.atlassian.net/browse/AND-5924
@Suppress("LargeClass")
internal class CurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    private val userWalletId: UserWalletId,
) {

    suspend fun getCurrenciesStatusesSync(): Either<Error, List<CryptoCurrencyStatus>> {
        return either {
            catch(
                block = {
                    val nonEmptyCurrencies =
                        currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId).toNonEmptyListOrNull()
                            ?: return emptyList<CryptoCurrencyStatus>().right()
                    val (networks, currenciesIds) = getIds(nonEmptyCurrencies)
                    val quotes = quotesRepository.getQuotesSync(currenciesIds, false).right()
                    val networkStatuses =
                        networksRepository.getNetworkStatusesSync(userWalletId, networks, false).right()
                    val yieldBalances = getYieldBalancesSync(nonEmptyCurrencies)
                    val addresses = getAddressesSync()

                    return createCurrenciesStatuses(
                        nonEmptyCurrencies,
                        quotes,
                        networkStatuses,
                        yieldBalances,
                        addresses
                    )
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    suspend fun getCurrencyStatusSync(
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
                    val quotes = quotesRepository.getQuoteSync(cryptoCurrencyId)?.right() ?: Error.EmptyQuotes.left()
                    val networkStatuses =
                        networksRepository.getNetworkStatusesSync(
                            userWalletId,
                            setOf(currency.network),
                            false,
                        ).firstOrNull {
                            it.network == currency.network
                        }.right()
                    val yieldBalances = getYieldBalanceSync(currency)

                    return createCurrencyStatus(currency, quotes, networkStatuses, yieldBalances)
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    suspend fun getNetworkCoinSync(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Either<Error, CryptoCurrencyStatus> {
        val currency = recover(
            block = { getNetworkCoin(networkId, derivationPath) },
            recover = { return it.left() },
        )

        return getCurrencyStatusSync(currency.id)
    }

    suspend fun getNetworkCoinForSingleWalletWithTokenSync(
        networkId: Network.ID,
    ): Either<Error, CryptoCurrencyStatus> = either {
        val currency = getNetworkCoinForSingleWalletWithToken(networkId)

        return getCurrencyStatusSync(currency.id)
    }

    suspend fun getPrimaryCurrencyStatusSync(): Either<Error, CryptoCurrencyStatus> = either {
        val currency = catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )
        val quotes = catch(
            block = { quotesRepository.getQuoteSync(currency.id)?.right() ?: Error.EmptyQuotes.left() },
            catch = { Error.DataError(it).left() },
        )
        val networkStatus = catch(
            block = {
                networksRepository.getNetworkStatusesSync(userWalletId, setOf(currency.network))
                    .firstOrNull { it.network == currency.network }
                    .right()
            },
            catch = { Error.DataError(it).left() },
        )
        val yieldBalances = getYieldBalanceSync(currency)

        return createCurrencyStatus(currency, quotes, networkStatus, yieldBalances)
    }

    fun getCardCurrenciesStatusesFlow(): Flow<Either<Error, List<CryptoCurrencyStatus>>> {
        return flow {
            val nonEmptyCurrencies = recover(
                block = { getCurrenciesFromCard(userWalletId) },
                recover = {
                    emit(it.left())
                    return@flow
                },
            ).toNonEmptyListOrNull()

            if (nonEmptyCurrencies == null) {
                val emptyCurrenciesStatuses = emptyList<CryptoCurrencyStatus>()

                emit(emptyCurrenciesStatuses.right())
                return@flow
            }

            val maybeLoadingCurrenciesStatuses = createCurrenciesStatuses(
                currencies = nonEmptyCurrencies,
                maybeNetworkStatuses = null,
                maybeQuotes = null,
                maybeYieldBalances = null,
                maybeAddresses = null,
            )

            emit(maybeLoadingCurrenciesStatuses)

            val (networks, currenciesIds) = getIds(nonEmptyCurrencies)

            val currenciesFlow = combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(networks),
                getYieldBalances(nonEmptyCurrencies),
                getAddresses(),
            ) { maybeQuotes, maybeNetworksStatuses, maybeYieldBalances, maybeAddresses ->
                createCurrenciesStatuses(
                    nonEmptyCurrencies,
                    maybeQuotes,
                    maybeNetworksStatuses,
                    maybeYieldBalances,
                    maybeAddresses,
                )
            }

            emitAll(currenciesFlow)
        }
    }

    suspend fun getCurrencyStatusFlow(currencyId: CryptoCurrency.ID): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getMultiCurrencyWalletCurrency(currencyId) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(currency)
    }

    suspend fun getCurrencyStatusSingleWalletWithTokensFlow(
        currencyId: CryptoCurrency.ID,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getSingleCurrencyWalletWithCardTokensCurrency(currencyId) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(currency)
    }

    suspend fun getNetworkCoinFlow(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getNetworkCoin(networkId, derivationPath) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(currency)
    }

    suspend fun getNetworkCoinForSingleWalletWithTokenFlow(
        networkId: Network.ID,
    ): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getNetworkCoinForSingleWalletWithToken(networkId) },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(currency)
    }

    suspend fun getPrimaryCurrencyStatusFlow(): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getPrimaryCurrency() },
            recover = { return flowOf(it.left()) },
        )

        return getCurrencyStatusFlow(currency)
    }

    fun getCurrencyStatusFlow(currency: CryptoCurrency): Flow<Either<Error, CryptoCurrencyStatus>> {
        val (networks, currenciesIds) = getIds(nonEmptyListOf(currency))

        val quoteFlow = getQuotes(currenciesIds)
            .map { maybeQuotes ->
                maybeQuotes.flatMap { quotes ->
                    quotes.singleOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }?.right()
                        ?: Error.EmptyQuotes.left()
                }
            }

        val statusFlow = getNetworksStatuses(networks)
            .map { maybeStatuses ->
                maybeStatuses.flatMap { statuses ->
                    statuses.singleOrNull { it.network == currency.network }?.right()
                        ?: Error.EmptyNetworksStatuses.left()
                }
            }

        val yieldBalanceFlow = getYieldBalance(currency)

        return combine(quoteFlow, statusFlow, yieldBalanceFlow) { maybeQuote, maybeNetworkStatus, maybeYieldBalance ->
            createCurrencyStatus(currency, maybeQuote, maybeNetworkStatus, maybeYieldBalance)
        }
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<Error, Set<Quote>>?,
        maybeNetworkStatuses: Either<Error, Set<NetworkStatus>>?,
        maybeYieldBalances: Either<Error, YieldBalanceList>?,
        maybeAddresses: Either<Error, List<CryptoCurrencyAddress>>?,
    ): Either<Error, List<CryptoCurrencyStatus>> = either {
        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bind()?.toNonEmptySetOrNull()
        val quotes: Set<Quote>? = maybeQuotes?.fold(
            ifLeft = {
                quotesRetrievingFailed = true
                null
            },
            ifRight = {
                it.ifEmpty {
                    quotesRetrievingFailed = true
                    null
                }
            },
        )

        val yieldBalances = maybeYieldBalances?.getOrNull()
        val addresses = maybeAddresses?.getOrNull()

        currencies.map { currency ->
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }
            val address = addresses?.find { it.cryptoCurrency == currency }

            val isStakingSupported = stakingRepository.isStakingSupported(
                stakingRepository.getIntegrationKey(currency.id),
            )
            val yieldBalance = if (isStakingSupported) {
                (yieldBalances as? YieldBalanceList.Data)?.getBalance(
                    address = address?.address,
                    rawCurrencyId = currency.id.rawCurrencyId,
                )
            } else {
                null
            }
            createCurrencyStatus(
                currency = currency,
                quote = quote,
                networkStatus = networkStatus,
                ignoreQuote = quotesRetrievingFailed,
                yieldBalance = yieldBalance,
            )
        }
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        maybeQuote: Either<Error, Quote?>,
        maybeNetworkStatus: Either<Error, NetworkStatus?>,
        maybeYieldBalance: Either<Error, YieldBalance>?,
    ): Either<Error, CryptoCurrencyStatus> = either {
        var quoteRetrievingFailed = false

        val networkStatus = maybeNetworkStatus.bind()
        val quote = recover({ maybeQuote.bind() }) {
            quoteRetrievingFailed = true
            null
        }
        val yieldBalance = maybeYieldBalance?.getOrNull()

        createCurrencyStatus(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            ignoreQuote = quoteRetrievingFailed,
            yieldBalance = yieldBalance,
        )
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
        ignoreQuote: Boolean,
        yieldBalance: YieldBalance?,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            ignoreQuote = ignoreQuote,
            yieldBalance = yieldBalance,
        )

        return currencyStatusOperations.createTokenStatus()
    }

    private suspend fun Raise<Error>.getMultiCurrencyWalletCurrency(currencyId: CryptoCurrency.ID): CryptoCurrency {
        return Either.catch {
            currenciesRepository.getMultiCurrencyWalletCurrency(
                userWalletId = userWalletId,
                id = currencyId,
            )
        }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun Raise<Error>.getSingleCurrencyWalletWithCardTokensCurrency(
        currencyId: CryptoCurrency.ID,
    ): CryptoCurrency {
        return Either.catch { currenciesRepository.getSingleCurrencyWalletWithCardCurrency(userWalletId, currencyId) }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun Raise<Error>.getNetworkCoin(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency {
        return Either.catch { currenciesRepository.getNetworkCoin(userWalletId, networkId, derivationPath) }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun Raise<Error>.getNetworkCoinForSingleWalletWithToken(networkId: Network.ID): CryptoCurrency {
        return Either.catch {
            currenciesRepository.getSingleCurrencyWalletWithCardCurrencies(userWalletId)
                .find { it.network.id == networkId && it is CryptoCurrency.Coin }
                ?: raise(Error.DataError(IllegalStateException("Coin with network $networkId not found for this card")))
        }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun Raise<Error>.getPrimaryCurrency(): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private suspend fun Raise<Error>.getCurrenciesFromCard(userWalletId: UserWalletId): List<CryptoCurrency> {
        return catch({ currenciesRepository.getSingleCurrencyWalletWithCardCurrencies(userWalletId) }) {
            raise(Error.DataError(it))
        }
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<Error, Set<Quote>>> {
        return quotesRepository.getQuotesUpdates(tokensIds)
            .map<Set<Quote>, Either<Error, Set<Quote>>> { quotes ->
                if (quotes.isEmpty()) Error.EmptyQuotes.left() else quotes.right()
            }
            .catch {
                emit(Error.DataError(it).left())
            }
    }

    private fun getNetworksStatuses(networks: NonEmptySet<Network>): Flow<Either<Error, Set<NetworkStatus>>> {
        return networksRepository.getNetworkStatusesUpdates(userWalletId, networks)
            .map<Set<NetworkStatus>, Either<Error, Set<NetworkStatus>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyNetworksStatuses.left()) }
    }

    private fun getYieldBalances(cryptoCurrencies: List<CryptoCurrency>): Flow<Either<Error, YieldBalanceList>> {
        return stakingRepository.getMultiYieldBalanceFlow(
            userWalletId = userWalletId,
            cryptoCurrencies = cryptoCurrencies,
        ).map<YieldBalanceList, Either<Error, YieldBalanceList>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyYieldBalances.left()) }
    }

    private fun getAddresses(): Flow<Either<Error, List<CryptoCurrencyAddress>>> {
        return networksRepository.getNetworkAddressesFlow(
            userWalletId = userWalletId,
        ).map<List<CryptoCurrencyAddress>, Either<Error, List<CryptoCurrencyAddress>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyAddresses.left()) }
    }

    private suspend fun getAddressesSync(): Either<Error, List<CryptoCurrencyAddress>> {
        return Either.catch {
            networksRepository.getNetworkAddresses(
                userWalletId = userWalletId,
            )
        }.mapLeft { Error.DataError(it) }
    }

    private suspend fun getYieldBalancesSync(
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

    private fun getYieldBalance(cryptoCurrency: CryptoCurrency): EitherFlow<Error, YieldBalance> {
        return stakingRepository.getSingleYieldBalanceFlow(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        ).map<YieldBalance, Either<Error, YieldBalance>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyYieldBalances.left()) }
    }

    private fun getIds(
        currencies: NonEmptyList<CryptoCurrency>,
    ): Pair<NonEmptySet<Network>, NonEmptySet<CryptoCurrency.ID>> {
        val currencyIdToNetworkId = currencies.associate { currency ->
            currency.id to currency.network
        }
        val currenciesIds = currencyIdToNetworkId.keys.toNonEmptySetOrNull()
        val networks = currencyIdToNetworkId.values.toNonEmptySetOrNull()

        requireNotNull(currenciesIds) { "Currencies IDs cannot be empty" }
        requireNotNull(networks) { "Networks IDs cannot be empty" }

        return networks to currenciesIds
    }

    sealed class Error {

        data object EmptyCurrencies : Error()

        data object EmptyQuotes : Error()

        data object EmptyNetworksStatuses : Error()

        data object EmptyAddresses : Error()

        data object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()

        data object EmptyYieldBalances : Error()
    }
}
