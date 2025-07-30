package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.withError
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class TokenListOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val userWalletId: UserWalletId,
    private val tokens: List<CryptoCurrencyStatus>,
) {

    fun getTokenListFlow(): Flow<Either<Error, TokenList>> {
        return combine(
            flow = getIsGrouped(),
            flow2 = getIsSortedByBalance(),
        ) { isGrouped, isSortedByBalance ->
            either {
                createTokenList(isGrouped = isGrouped.bind(), isSortedByBalance = isSortedByBalance.bind())
            }
        }
    }

    private fun Raise<Error>.createTokenList(isGrouped: Boolean, isSortedByBalance: Boolean): TokenList {
        val nonEmptyCurrencies = tokens.toNonEmptyListOrNull() ?: return TokenList.Empty

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
        fiatBalance: TotalFiatBalance,
        isAnyTokenLoading: Boolean,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): TokenList {
        val sortingOperations = TokenListSortingOperations(
            currencies = currencies,
            isAnyTokenLoading = isAnyTokenLoading,
            sortByBalance = isSortedByBalance,
        )

        return createTokenList(sortingOperations, fiatBalance, isGrouped)
    }

    private fun Raise<Error>.createTokenList(
        sortingOperations: TokenListSortingOperations,
        fiatBalance: TotalFiatBalance,
        isGrouped: Boolean,
    ): TokenList {
        return if (isGrouped) {
            createGroupedTokenList(sortingOperations, fiatBalance)
        } else {
            createUngroupedTokenList(sortingOperations, fiatBalance)
        }
    }

    private fun Raise<Error>.createUngroupedTokenList(
        sortingOperations: TokenListSortingOperations,
        fiatBalance: TotalFiatBalance,
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
        fiatBalance: TotalFiatBalance,
    ): TokenList.GroupedByNetwork = TokenList.GroupedByNetwork(
        sortedBy = sortingOperations.getSortType(),
        totalFiatBalance = fiatBalance,
        groups = withError(
            transform = { e ->
                Error.fromTokenListOperations(e) { createUnsortedUngroupedTokenList(tokens, fiatBalance) }
            },
            block = { sortingOperations.getGroupedTokens().bind() },
        ),
    )

    private fun createUnsortedUngroupedTokenList(
        tokens: List<CryptoCurrencyStatus>,
        fiatBalance: TotalFiatBalance,
    ): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            sortedBy = TokensSortType.NONE,
            totalFiatBalance = fiatBalance,
            currencies = tokens,
        )
    }

    private fun getIsGrouped(): Flow<Either<Error, Boolean>> {
        return currenciesRepository.isTokensGrouped(userWalletId)
            .map<Boolean, Either<Error, Boolean>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(value = false.right()) }
            .cancellable()
    }

    private fun getIsSortedByBalance(): Flow<Either<Error, Boolean>> {
        return currenciesRepository.isTokensSortedByBalance(userWalletId)
            .map<Boolean, Either<Error, Boolean>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(value = false.right()) }
            .cancellable()
    }

    sealed class Error {

        data class UnableToSortTokenList(val unsortedTokenList: TokenList.Ungrouped) : Error()

        data class DataError(val cause: Throwable) : Error()

        internal companion object {

            fun fromTokenListOperations(
                e: TokenListSortingOperations.Error,
                createUnsortedUngroupedTokenList: () -> TokenList.Ungrouped,
            ): Error = when (e) {
                is TokenListSortingOperations.Error.EmptyTokens,
                is TokenListSortingOperations.Error.NetworkNotFound,
                -> UnableToSortTokenList(
                    unsortedTokenList = createUnsortedUngroupedTokenList(),
                )
            }
        }
    }
}