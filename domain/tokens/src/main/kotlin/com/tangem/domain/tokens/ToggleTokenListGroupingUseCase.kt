package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.withError
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.error.mapper.mapToTokenListSortingError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.tokens.operations.TokenListSortingOperations
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class ToggleTokenListGroupingUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(tokenList: TokenList): Either<TokenListSortingError, TokenList> {
        return withContext(dispatchers.default) {
            either {
                when (tokenList) {
                    is TokenList.GroupedByNetwork -> ungroupTokens(tokenList)
                    is TokenList.Ungrouped -> groupTokens(tokenList)
                    is TokenList.Empty -> raise(TokenListSortingError.TokenListIsEmpty)
                }
            }
        }
    }

    private fun Raise<TokenListSortingError>.groupTokens(tokenList: TokenList.Ungrouped): TokenList.GroupedByNetwork {
        ensure(tokenList.totalFiatBalance !is TotalFiatBalance.Loading) {
            TokenListSortingError.TokenListIsLoading
        }

        val sortingOperations = TokenListSortingOperations(tokenList)

        return TokenList.GroupedByNetwork(
            groups = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
                sortingOperations.getGroupedTokens().bind()
            },
            totalFiatBalance = tokenList.totalFiatBalance,
            sortedBy = sortingOperations.getSortType(),
        )
    }

    private fun Raise<TokenListSortingError>.ungroupTokens(
        tokenList: TokenList.GroupedByNetwork,
    ): TokenList.Ungrouped {
        ensure(tokenList.totalFiatBalance !is TotalFiatBalance.Loading) {
            TokenListSortingError.TokenListIsLoading
        }

        val sortingOperations = TokenListSortingOperations(tokenList)

        return TokenList.Ungrouped(
            currencies = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
                sortingOperations.getTokens().bind()
            },
            totalFiatBalance = tokenList.totalFiatBalance,
            sortedBy = sortingOperations.getSortType(),
        )
    }
}