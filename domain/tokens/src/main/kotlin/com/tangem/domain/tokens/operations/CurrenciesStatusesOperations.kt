package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.tangem.domain.core.raise.DelegatedRaise
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class CurrenciesStatusesOperations<E>(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val refresh: Boolean,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<E>,
    transformError: (Error) -> E,
) : DelegatedRaise<CurrenciesStatusesOperations.Error, E>(raise, transformError) {

    constructor(
        userWalletId: UserWalletId,
        refresh: Boolean,
        useCase: GetTokenListUseCase,
        raise: Raise<E>,
        transformError: (Error) -> E,
    ) : this(
        currenciesRepository = useCase.currenciesRepository,
        quotesRepository = useCase.quotesRepository,
        networksRepository = useCase.networksRepository,
        userWalletId = userWalletId,
        refresh = refresh,
        dispatchers = useCase.dispatchers,
        raise = raise,
        transformError = transformError,
    )

    fun getCurrenciesStatusesFlow(): Flow<Set<CryptoCurrencyStatus>> {
        return getMultiCurrencyWalletCurrencies().flatMapConcat {
            val currencies = it.toNonEmptySetOrNull()

            if (currencies == null) {
                flowOf(emptySet())
            } else {
                val currencyIdToNetworkId = currencies.associate { currency ->
                    currency.id to currency.networkId
                }
                val currenciesIds = requireNotNull(currencyIdToNetworkId.keys.toNonEmptySetOrNull()) {
                    "Currencies IDs cannot be empty"
                }
                val networksIds = requireNotNull(currencyIdToNetworkId.values.toNonEmptySetOrNull()) {
                    "Networks IDs cannot be empty"
                }

                combine(getQuotes(currenciesIds), getNetworksStatues(networksIds)) { quotes, networksStatuses ->
                    createTokensStatuses(currencies, quotes, networksStatuses)
                }
            }
        }
    }

    suspend fun getCurrencyStatusFlow(currencyId: CryptoCurrency.ID): Flow<CryptoCurrencyStatus> {
        val currency = getMultiCurrencyWalletCurrency(currencyId)

        return getCurrencyStatusFlow(currency)
    }

    suspend fun getPrimaryCurrencyStatusFlow(): Flow<CryptoCurrencyStatus> {
        val currency = getPrimaryCurrency()

        return getCurrencyStatusFlow(currency)
    }

    private fun getCurrencyStatusFlow(currency: CryptoCurrency): Flow<CryptoCurrencyStatus> {
        val quoteFlow = getQuotes(nonEmptySetOf(currency.id))
            .map { quotes ->
                quotes.singleOrNull { it.currencyId == currency.id }
            }

        val statusFlow = getNetworksStatues(nonEmptySetOf(currency.networkId))
            .map { statuses ->
                statuses.singleOrNull { it.networkId == currency.networkId }
            }

        return combine(quoteFlow, statusFlow) { quote, networkStatus ->
            createStatus(currency, quote, networkStatus)
        }
    }

    private suspend fun createTokensStatuses(
        tokens: Set<CryptoCurrency>,
        quotes: Set<Quote>,
        networkStatuses: Set<NetworkStatus>,
    ): Set<CryptoCurrencyStatus> = withContext(dispatchers.default) {
        tokens.mapTo(hashSetOf()) { token ->
            val quote = quotes.firstOrNull { it.currencyId == token.id }
            val networkStatus = networkStatuses.firstOrNull { it.networkId == token.networkId }

            createStatus(token, quote, networkStatus)
        }
    }

    private suspend fun createStatus(
        token: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = token,
            quote = quote,
            networkStatus = networkStatus,
            dispatchers = dispatchers,
            raise = this,
            transformError = { Error.UnableToCreateCurrencyStatus },
        )

        return currencyStatusOperations.createTokenStatus()
    }

    private suspend fun getMultiCurrencyWalletCurrency(currencyId: CryptoCurrency.ID): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, currencyId) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private fun getMultiCurrencyWalletCurrencies(): Flow<Set<CryptoCurrency>> {
        return currenciesRepository.getMultiCurrencyWalletCurrencies(userWalletId, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyCurrencies) }
    }

    private suspend fun getPrimaryCurrency(): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Set<Quote>> {
        return quotesRepository.getQuotes(tokensIds, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyQuotes) }
    }

    private fun getNetworksStatues(networks: NonEmptySet<Network.ID>): Flow<Set<NetworkStatus>> {
        return networksRepository.getNetworkStatuses(userWalletId, networks, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyNetworksStatuses) }
    }

    sealed class Error {

        object EmptyCurrencies : Error()

        object EmptyQuotes : Error()

        object EmptyNetworksStatuses : Error()

        object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()
    }
}