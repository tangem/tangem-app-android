package com.tangem.domain.tokens.operations

import arrow.core.NonEmptySet
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.core.raise.DelegatedRaise
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class TokenListOperations<E>(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val tokens: Set<CryptoCurrencyStatus>,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<E>,
    transform: (Error) -> E,
) : DelegatedRaise<TokenListOperations.Error, E>(raise, transform) {

    constructor(
        userWalletId: UserWalletId,
        tokens: Set<CryptoCurrencyStatus>,
        useCase: GetTokenListUseCase,
        raise: Raise<E>,
        transform: (Error) -> E,
    ) : this(
        currenciesRepository = useCase.currenciesRepository,
        networksRepository = useCase.networksRepository,
        userWalletId = userWalletId,
        tokens = tokens,
        dispatchers = useCase.dispatchers,
        raise = raise,
        transform = transform,
    )

    fun getTokenListFlow(): Flow<TokenList> {
        return combine(getIsGrouped(), getIsSortedByBalance()) { isGrouped, isSortedByBalance ->
            createTokenList(isGrouped, isSortedByBalance)
        }
    }

    private suspend fun createTokenList(isGrouped: Boolean, isSortedByBalance: Boolean): TokenList {
        return withContext(dispatchers.default) {
            val tokensNes = tokens.toNonEmptySetOrNull()
                ?: return@withContext TokenList.NotInitialized

            val isAnyTokenLoading = tokensNes.any { it.value is CryptoCurrencyStatus.Loading }
            val fiatBalanceOperations = TokenListFiatBalanceOperations(tokensNes, isAnyTokenLoading, dispatchers)

            createTokenList(
                tokens = tokensNes,
                fiatBalance = fiatBalanceOperations.calculateFiatBalance(),
                isAnyTokenLoading = isAnyTokenLoading,
                isGrouped = isGrouped,
                isSortedByBalance = isSortedByBalance,
            )
        }
    }

    private suspend fun createTokenList(
        tokens: NonEmptySet<CryptoCurrencyStatus>,
        fiatBalance: TokenList.FiatBalance,
        isAnyTokenLoading: Boolean,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): TokenList {
        val sortingOperations = TokenListSortingOperations(
            currencies = tokens,
            isAnyTokenLoading = isAnyTokenLoading,
            sortByBalance = isSortedByBalance,
            dispatchers = dispatchers,
            raise = this,
            transformError = { e ->
                Error.fromTokenListOperations(e) { createUnsortedUngroupedTokenList(tokens, fiatBalance) }
            },
        )

        return createTokenList(tokens, sortingOperations, fiatBalance, isGrouped)
    }

    private suspend fun createTokenList(
        tokens: NonEmptySet<CryptoCurrencyStatus>,
        sortingOperations: TokenListSortingOperations<*>,
        fiatBalance: TokenList.FiatBalance,
        isGrouped: Boolean,
    ): TokenList {
        return if (isGrouped) {
            val networks = ensureNotNull(getNetworks(tokens).toNonEmptySetOrNull()) {
                Error.UnableToGroupTokenList(
                    ungroupedTokenList = createUngroupedTokenList(sortingOperations, fiatBalance),
                )
            }

            createGroupedTokenList(sortingOperations, fiatBalance, networks)
        } else {
            createUngroupedTokenList(sortingOperations, fiatBalance)
        }
    }

    private suspend fun getNetworks(tokensNes: NonEmptySet<CryptoCurrencyStatus>): Set<Network> {
        return withContext(dispatchers.io) {
            val networksIds = tokensNes.map { it.currency.networkId }.toNonEmptySet()
            catch(
                block = { networksRepository.getNetworks(networksIds) },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    private suspend fun createUngroupedTokenList(
        sortingOperations: TokenListSortingOperations<*>,
        fiatBalance: TokenList.FiatBalance,
    ): TokenList.Ungrouped = TokenList.Ungrouped(
        sortedBy = sortingOperations.getSortType(),
        totalFiatBalance = fiatBalance,
        currencies = sortingOperations.getTokens(),
    )

    private suspend fun createGroupedTokenList(
        sortingOperations: TokenListSortingOperations<*>,
        fiatBalance: TokenList.FiatBalance,
        networks: NonEmptySet<Network>,
    ): TokenList.GroupedByNetwork = TokenList.GroupedByNetwork(
        sortedBy = sortingOperations.getSortType(),
        totalFiatBalance = fiatBalance,
        groups = sortingOperations.getGroupedTokens(networks),
    )

    private fun createUnsortedUngroupedTokenList(
        tokens: NonEmptySet<CryptoCurrencyStatus>,
        fiatBalance: TokenList.FiatBalance,
    ): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            sortedBy = TokenList.SortType.NONE,
            totalFiatBalance = fiatBalance,
            currencies = tokens,
        )
    }

    private fun getIsGrouped(): Flow<Boolean> {
        return currenciesRepository.isTokensGrouped(userWalletId)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { emit(value = false) }
            .flowOn(dispatchers.io)
    }

    private fun getIsSortedByBalance(): Flow<Boolean> {
        return currenciesRepository.isTokensSortedByBalance(userWalletId)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { emit(value = false) }
            .flowOn(dispatchers.io)
    }

    sealed class Error {

        data class UnableToSortTokenList(val unsortedTokenList: TokenList.Ungrouped) : Error()

        data class UnableToGroupTokenList(val ungroupedTokenList: TokenList.Ungrouped) : Error()

        data class DataError(val cause: Throwable) : Error()

        internal companion object {

            fun fromTokenListOperations(
                e: TokenListSortingOperations.Error,
                createUnsortedUngroupedTokenList: () -> TokenList.Ungrouped,
            ): Error = when (e) {
                is TokenListSortingOperations.Error.EmptyNetworks,
                is TokenListSortingOperations.Error.EmptyTokens,
                is TokenListSortingOperations.Error.NetworkNotFound,
                -> UnableToSortTokenList(
                    unsortedTokenList = createUnsortedUngroupedTokenList(),
                )
            }
        }
    }
}
