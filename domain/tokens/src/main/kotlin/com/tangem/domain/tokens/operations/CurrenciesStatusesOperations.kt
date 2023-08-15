package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.models.Quote
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class CurrenciesStatusesOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val refresh: Boolean,
) {

    constructor(
        userWalletId: UserWalletId,
        refresh: Boolean,
        useCase: GetTokenListUseCase,
    ) : this(
        currenciesRepository = useCase.currenciesRepository,
        quotesRepository = useCase.quotesRepository,
        networksRepository = useCase.networksRepository,
        userWalletId = userWalletId,
        refresh = refresh,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCurrenciesStatusesFlow(): Flow<Either<Error, List<CryptoCurrencyStatus>>> {
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
            } else if (!refresh) {
                val maybeLoadingCurrenciesStatuses = createCurrenciesStatuses(
                    currencies = nonEmptyCurrencies,
                    maybeNetworkStatuses = null,
                    maybeQuotes = null,
                )

                emit(maybeLoadingCurrenciesStatuses)
            }

            val (networksIds, currenciesIds) = getIds(nonEmptyCurrencies)

            val currenciesFlow = combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(networksIds),
            ) { maybeQuotes, maybeNetworksStatuses ->
                createCurrenciesStatuses(nonEmptyCurrencies, maybeQuotes, maybeNetworksStatuses)
            }

            emitAll(currenciesFlow)
        }.conflate()
    }

    suspend fun getCurrencyStatusFlow(currencyId: CryptoCurrency.ID): Flow<Either<Error, CryptoCurrencyStatus>> {
        val currency = recover(
            block = { getMultiCurrencyWalletCurrency(currencyId) },
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
        val (networksIds, currenciesIds) = getIds(nonEmptyListOf(currency))

        val quoteFlow = getQuotes(currenciesIds)
            .map { maybeQuotes ->
                maybeQuotes.map { quotes ->
                    quotes.singleOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
                }
            }

        val statusFlow = getNetworksStatuses(networksIds)
            .map { maybeStatuses ->
                maybeStatuses.map { statuses ->
                    statuses.singleOrNull { it.networkId == currency.networkId }
                }
            }

        return combine(quoteFlow, statusFlow) { maybeQuote, maybeNetworkStatus ->
            createStatus(currency, maybeQuote, maybeNetworkStatus)
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
            val networkStatus = networksStatuses?.firstOrNull { it.networkId == currency.networkId }

            createStatus(currency, quote, networkStatus, ignoreQuote = quotesRetrievingFailed)
        }
    }

    private fun createStatus(
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

        createStatus(currency, quote, networkStatus, ignoreQuote = quoteRetrievingFailed)
    }

    private fun createStatus(
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
        return currenciesRepository.getMultiCurrencyWalletCurrencies(userWalletId, refresh)
            .map<List<CryptoCurrency>, Either<Error, List<CryptoCurrency>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyCurrencies.left()) }
    }

    private suspend fun Raise<Error>.getMultiCurrencyWalletCurrency(currencyId: CryptoCurrency.ID): CryptoCurrency {
        return Either.catch { currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, currencyId) }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun Raise<Error>.getPrimaryCurrency(): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<Error, Set<Quote>>> {
        return quotesRepository.getQuotes(tokensIds, refresh)
            .map<Set<Quote>, Either<Error, Set<Quote>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyQuotes.left()) }
    }

    private fun getNetworksStatuses(networks: NonEmptySet<Network.ID>): Flow<Either<Error, Set<NetworkStatus>>> {
        return networksRepository.getNetworkStatuses(userWalletId, networks, refresh)
            .map<Set<NetworkStatus>, Either<Error, Set<NetworkStatus>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyNetworksStatuses.left()) }
    }

    private fun getIds(
        currencies: NonEmptyList<CryptoCurrency>,
    ): Pair<NonEmptySet<Network.ID>, NonEmptySet<CryptoCurrency.ID>> {
        val currencyIdToNetworkId = currencies.associate { currency ->
            currency.id to currency.networkId
        }
        val currenciesIds = currencyIdToNetworkId.keys.toNonEmptySetOrNull()
        val networksIds = currencyIdToNetworkId.values.toNonEmptySetOrNull()

        requireNotNull(currenciesIds) { "Currencies IDs cannot be empty" }
        requireNotNull(networksIds) { "Networks IDs cannot be empty" }

        return networksIds to currenciesIds
    }

    sealed class Error {

        object EmptyCurrencies : Error()

        object EmptyQuotes : Error()

        object EmptyNetworksStatuses : Error()

        object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()
    }
}
