package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

// FIXME: Refactor - [REDACTED_JIRA]
@Suppress("LargeClass")
internal class CurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCurrenciesStatusesFlow(): EitherFlow<Error, List<CryptoCurrencyStatus>> {
        return getMultiCurrencyWalletCurrencies().transformLatest { maybeCurrencies ->
            val nonEmptyCurrencies = maybeCurrencies.fold(
                ifLeft = { error ->
                    emit(error.left())
                    return@transformLatest
                },
                ifRight = List<CryptoCurrency>::toNonEmptyListOrNull,
            )

            if (nonEmptyCurrencies == null) {
                val emptyCurrenciesStatuses = emptyList<CryptoCurrencyStatus>()

                emit(emptyCurrenciesStatuses.right())
                return@transformLatest
            }

            val maybeLoadingCurrenciesStatuses = createCurrenciesStatuses(
                currencies = nonEmptyCurrencies,
                maybeNetworkStatuses = null,
                maybeQuotes = null,
            )

            emit(maybeLoadingCurrenciesStatuses)

            val (networks, currenciesIds) = getIds(nonEmptyCurrencies)

            val currenciesFlow = combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(networks),
            ) { maybeQuotes, maybeNetworksStatuses ->
                createCurrenciesStatuses(nonEmptyCurrencies, maybeQuotes, maybeNetworksStatuses)
            }

            emitAll(currenciesFlow)
        }
    }

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
                    return createCurrenciesStatuses(nonEmptyCurrencies, quotes, networkStatuses)
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    suspend fun getCurrencyStatusSync(cryptoCurrencyId: CryptoCurrency.ID): Either<Error, CryptoCurrencyStatus> {
        return either {
            catch(
                block = {
                    val currency =
                        currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, cryptoCurrencyId)
                    val quotes = quotesRepository.getQuoteSync(cryptoCurrencyId).right()
                    val networkStatuses =
                        networksRepository.getNetworkStatusesSync(
                            userWalletId,
                            setOf(currency.network),
                            false,
                        ).firstOrNull {
                            it.network == currency.network
                        }.right()
                    return createCurrencyStatus(currency, quotes, networkStatuses)
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

    suspend fun getPrimaryCurrencyStatusSync(): Either<Error, CryptoCurrencyStatus> = either {
        val currency = catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )
        val quotes = catch(
            block = { quotesRepository.getQuoteSync(currency.id).right() },
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

        return createCurrencyStatus(currency, quotes, networkStatus)
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
            )

            emit(maybeLoadingCurrenciesStatuses)

            val (networks, currenciesIds) = getIds(nonEmptyCurrencies)

            val currenciesFlow = combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(networks),
            ) { maybeQuotes, maybeNetworksStatuses ->
                createCurrenciesStatuses(nonEmptyCurrencies, maybeQuotes, maybeNetworksStatuses)
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

    private fun getCurrencyStatusFlow(currency: CryptoCurrency): Flow<Either<Error, CryptoCurrencyStatus>> {
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

        return combine(quoteFlow, statusFlow) { maybeQuote, maybeNetworkStatus ->
            createCurrencyStatus(currency, maybeQuote, maybeNetworkStatus)
        }
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<Error, Set<Quote>>?,
        maybeNetworkStatuses: Either<Error, Set<NetworkStatus>>?,
    ): Either<Error, List<CryptoCurrencyStatus>> = either {
        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bind()?.toNonEmptySetOrNull()
        val quotes = recover({ maybeQuotes?.bind()?.toNonEmptySetOrNull() }) {
            quotesRetrievingFailed = true
            null
        }

        currencies.map { currency ->
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }

            createCurrencyStatus(currency, quote, networkStatus, ignoreQuote = quotesRetrievingFailed)
        }
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        maybeQuote: Either<Error, Quote?>,
        maybeNetworkStatus: Either<Error, NetworkStatus?>,
    ): Either<Error, CryptoCurrencyStatus> = either {
        var quoteRetrievingFailed = false

        val networkStatus = maybeNetworkStatus.bind()
        val quote = recover({ maybeQuote.bind() }) {
            quoteRetrievingFailed = true
            null
        }

        createCurrencyStatus(currency, quote, networkStatus, ignoreQuote = quoteRetrievingFailed)
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
        ignoreQuote: Boolean,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            ignoreQuote = ignoreQuote,
        )

        return currencyStatusOperations.createTokenStatus()
    }

    private fun getMultiCurrencyWalletCurrencies(): Flow<Either<Error, List<CryptoCurrency>>> {
        return currenciesRepository.getMultiCurrencyWalletCurrenciesUpdates(userWalletId)
            .map<List<CryptoCurrency>, Either<Error, List<CryptoCurrency>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyCurrencies.left()) }
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

        object EmptyCurrencies : Error()

        object EmptyQuotes : Error()

        object EmptyNetworksStatuses : Error()

        object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()
    }
}