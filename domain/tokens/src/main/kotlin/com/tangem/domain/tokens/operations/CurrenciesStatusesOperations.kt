package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.*
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
        return getMultiCurrencyWalletCurrencies().flatMapMerge flatMap@{ maybeCurrencies ->
            val nonEmptyCurrencies = maybeCurrencies.fold(
                ifLeft = { error ->
                    return@flatMap flowOf(error.left())
                },
                ifRight = { it.toNonEmptyListOrNull() },
            ) ?: return@flatMap flowOf(emptyList<CryptoCurrencyStatus>().right())

            val (networksIds, currenciesIds) = getIds(nonEmptyCurrencies)

            combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(networksIds),
            ) { maybeQuotes, maybeNetworksStatuses ->
                either {
                    createCurrenciesStatuses(nonEmptyCurrencies, maybeQuotes.bind(), maybeNetworksStatuses.bind())
                }
            }
        }
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
        val quoteFlow = getQuotes(nonEmptySetOf(currency.id))
            .map { maybeQuotes ->
                maybeQuotes.map { quotes ->
                    quotes.singleOrNull { it.currencyId == currency.id }
                }
            }

        val statusFlow = getNetworksStatuses(nonEmptySetOf(currency.networkId))
            .map { maybeStatuses ->
                maybeStatuses.map { statuses ->
                    statuses.singleOrNull { it.networkId == currency.networkId }
                }
            }

        return combine(quoteFlow, statusFlow) { maybeQuote, maybeNetworkStatus ->
            either {
                createStatus(currency, maybeQuote.bind(), maybeNetworkStatus.bind())
            }
        }
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        quotes: Set<Quote>,
        networkStatuses: Set<NetworkStatus>,
    ): List<CryptoCurrencyStatus> {
        return currencies.map { token ->
            val quote = quotes.firstOrNull { it.currencyId == token.id }
            val networkStatus = networkStatuses.firstOrNull { it.networkId == token.networkId }

            createStatus(token, quote, networkStatus)
        }
    }

    private fun createStatus(
        token: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = token,
            quote = quote,
            networkStatus = networkStatus,
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