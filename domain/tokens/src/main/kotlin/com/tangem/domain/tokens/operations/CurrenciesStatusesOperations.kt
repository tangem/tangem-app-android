package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.tangem.domain.core.raise.DelegatedRaise
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class CurrenciesStatusesOperations<E>(
    private val tokensRepository: TokensRepository,
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
        tokensRepository = useCase.tokensRepository,
        quotesRepository = useCase.quotesRepository,
        networksRepository = useCase.networksRepository,
        userWalletId = userWalletId,
        refresh = refresh,
        dispatchers = useCase.dispatchers,
        raise = raise,
        transformError = transformError,
    )

    fun getMultiCurrencyWalletStatusesFlow(): Flow<Set<CryptoCurrencyStatus>> {
        return getMultiCurrencyWalletCurrencies().flatMapConcat {
            val tokens = it.toNonEmptySetOrNull()

            if (tokens == null) {
                flowOf(emptySet())
            } else {
                val tokensIds = tokens.map { token -> token.id }.toNonEmptySet()
                val groupedTokens = groupTokens(tokens)

                combine(getQuotes(tokensIds), getNetworksStatues(groupedTokens)) { quotes, networksStatuses ->
                    createTokensStatuses(tokens, quotes, networksStatuses)
                }
            }
        }
    }

    suspend fun getPrimaryCurrencyStatusFlow(): Flow<CryptoCurrencyStatus> {
        val token = getPrimaryCurrency()

        val quoteFlow = getQuotes(nonEmptySetOf(token.id))
            .map { quotes ->
                quotes.singleOrNull { it.currencyId == token.id }
            }

        val statusFlow = getNetworksStatues(groupTokens(nonEmptySetOf(token)))
            .map { statuses ->
                statuses.singleOrNull { it.networkId == token.networkId }
            }

        return combine(quoteFlow, statusFlow) { quote, networkStatus ->
            createStatus(token, quote, networkStatus)
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

    private fun getMultiCurrencyWalletCurrencies(): Flow<Set<CryptoCurrency>> {
        return tokensRepository.getMultiCurrencyWalletCurrencies(userWalletId, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyCurrencies) }
            .flowOn(dispatchers.io)
    }

    private suspend fun getPrimaryCurrency(): CryptoCurrency {
        return withContext(dispatchers.io) {
            catch(
                block = { tokensRepository.getPrimaryCurrency(userWalletId) },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Set<Quote>> {
        return quotesRepository.getQuotes(tokensIds, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyQuotes) }
            .flowOn(dispatchers.io)
    }

    private fun getNetworksStatues(
        groupedTokens: Map<Network.ID, NonEmptySet<CryptoCurrency.ID>>,
    ): Flow<Set<NetworkStatus>> {
        return networksRepository.getNetworkStatuses(userWalletId, groupedTokens, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyNetworksStatuses) }
            .flowOn(dispatchers.io)
    }

    private suspend fun groupTokens(
        tokens: NonEmptySet<CryptoCurrency>,
    ): Map<Network.ID, NonEmptySet<CryptoCurrency.ID>> {
        return withContext(dispatchers.default) {
            tokens
                .groupBy { it.networkId }
                .mapValues { (_, tokens) ->
                    // Can not be empty
                    tokens.toNonEmptySetOrNull()!!
                        .map { it.id }
                        .toNonEmptySet()
                }
        }
    }

    sealed class Error {

        object EmptyCurrencies : Error()

        object EmptyQuotes : Error()

        object EmptyNetworksStatuses : Error()

        object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()
    }
}
