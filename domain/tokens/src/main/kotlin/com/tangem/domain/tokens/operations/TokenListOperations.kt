package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class TokenListOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val tokens: List<CryptoCurrencyStatus>,
) {

    constructor(
        userWalletId: UserWalletId,
        tokens: List<CryptoCurrencyStatus>,
        useCase: GetTokenListUseCase,
    ) : this(
        currenciesRepository = useCase.currenciesRepository,
        networksRepository = useCase.networksRepository,
        userWalletId = userWalletId,
        tokens = tokens,
    )

    fun getTokenListFlow(): Flow<Either<Error, TokenList>> {
        return combine(
            getIsGrouped(),
            getIsSortedByBalance(),
        ) { isGrouped, isSortedByBalance ->
            either {
                createTokenList(isGrouped.bind(), isSortedByBalance.bind())
            }
        }
    }

    private fun Raise<Error>.createTokenList(isGrouped: Boolean, isSortedByBalance: Boolean): TokenList {
        val nonEmptyCurrencies = tokens.toNonEmptyListOrNull()
            ?: return TokenList.NotInitialized

        val isAnyTokenLoading = nonEmptyCurrencies.any { it.value is CryptoCurrencyStatus.Loading }
        val fiatBalanceOperations = TokenListFiatBalanceOperations(nonEmptyCurrencies, isAnyTokenLoading)

        return createTokenList(
            currencies = nonEmptyCurrencies,
            fiatBalance = fiatBalanceOperations.calculateFiatBalance(),
            isAnyTokenLoading = isAnyTokenLoading,
            isGrouped = isGrouped,
            isSortedByBalance = isSortedByBalance,
        )
    }

    private fun Raise<Error>.createTokenList(
        currencies: NonEmptyList<CryptoCurrencyStatus>,
        fiatBalance: TokenList.FiatBalance,
        isAnyTokenLoading: Boolean,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): TokenList {
        val sortingOperations = TokenListSortingOperations(
            currencies = currencies,
            isAnyTokenLoading = isAnyTokenLoading,
            sortByBalance = isSortedByBalance,
        )

        return createTokenList(currencies, sortingOperations, fiatBalance, isGrouped)
    }

    private fun Raise<Error>.createTokenList(
        tokens: NonEmptyList<CryptoCurrencyStatus>,
        sortingOperations: TokenListSortingOperations,
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

    private fun Raise<Error>.getNetworks(tokensNes: NonEmptyList<CryptoCurrencyStatus>): Set<Network> {
        val networksIds = tokensNes.map { it.currency.networkId }.toNonEmptySet()

        return catch(
            block = { networksRepository.getNetworks(networksIds) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private fun Raise<Error>.createUngroupedTokenList(
        sortingOperations: TokenListSortingOperations,
        fiatBalance: TokenList.FiatBalance,
    ): TokenList.Ungrouped = TokenList.Ungrouped(
        sortedBy = sortingOperations.getSortType(),
        totalFiatBalance = fiatBalance,
        currencies = withError(
            transform = { e ->
                Error.fromTokenListOperations(e) { createUnsortedUngroupedTokenList(tokens, fiatBalance) }
            },
            block = { sortingOperations.getTokens().bind() },
        ),
    )

    private fun Raise<Error>.createGroupedTokenList(
        sortingOperations: TokenListSortingOperations,
        fiatBalance: TokenList.FiatBalance,
        networks: NonEmptySet<Network>,
    ): TokenList.GroupedByNetwork = TokenList.GroupedByNetwork(
        sortedBy = sortingOperations.getSortType(),
        totalFiatBalance = fiatBalance,
        groups = withError(
            transform = { e ->
                Error.fromTokenListOperations(e) { createUnsortedUngroupedTokenList(tokens, fiatBalance) }
            },
            block = { sortingOperations.getGroupedTokens(networks).bind() },
        ),
    )

    private fun createUnsortedUngroupedTokenList(
        tokens: List<CryptoCurrencyStatus>,
        fiatBalance: TokenList.FiatBalance,
    ): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            sortedBy = TokenList.SortType.NONE,
            totalFiatBalance = fiatBalance,
            currencies = tokens,
        )
    }

    private fun getIsGrouped(): Flow<Either<Error, Boolean>> {
        return currenciesRepository.isTokensGrouped(userWalletId)
            .map<Boolean, Either<Error, Boolean>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(value = false.right()) }
    }

    private fun getIsSortedByBalance(): Flow<Either<Error, Boolean>> {
        return currenciesRepository.isTokensSortedByBalance(userWalletId)
            .map<Boolean, Either<Error, Boolean>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(value = false.right()) }
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