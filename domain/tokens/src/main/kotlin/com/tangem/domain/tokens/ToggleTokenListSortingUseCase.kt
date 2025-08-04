package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.withError
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.error.mapper.mapToTokenListSortingError
import com.tangem.domain.tokens.operations.TokenListSortingOperations
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class ToggleTokenListSortingUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(tokenList: TokenList): Either<TokenListSortingError, TokenList> {
        return withContext(dispatchers.default) {
            either {
                when (tokenList) {
                    is TokenList.GroupedByNetwork -> sortGroupedTokenList(tokenList)
                    is TokenList.Ungrouped -> sortUngroupedTokenList(tokenList)
                    is TokenList.Empty -> raise(TokenListSortingError.TokenListIsEmpty)
                }
            }
        }
    }

    private fun Raise<TokenListSortingError>.sortGroupedTokenList(
        tokenList: TokenList.GroupedByNetwork,
    ): TokenList.GroupedByNetwork {
        ensure(tokenList.totalFiatBalance !is TotalFiatBalance.Loading) {
            TokenListSortingError.TokenListIsLoading
        }

        val operations = getSortingOperations(tokenList)

        return tokenList.copy(
            groups = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
                operations.getGroupedTokens().bind()
            },
            sortedBy = operations.getSortType(),
        )
    }

    private fun Raise<TokenListSortingError>.sortUngroupedTokenList(
        tokenList: TokenList.Ungrouped,
    ): TokenList.Ungrouped {
        ensure(tokenList.totalFiatBalance !is TotalFiatBalance.Loading) {
            TokenListSortingError.TokenListIsLoading
        }

        val operations = getSortingOperations(tokenList)

        return tokenList.copy(
            currencies = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
                operations.getTokens().bind()
            },
            sortedBy = operations.getSortType(),
        )
    }

    private fun getSortingOperations(tokenList: TokenList): TokenListSortingOperations {
        return TokenListSortingOperations(
            tokenList = tokenList,
            sortByBalance = tokenList.sortedBy != TokensSortType.BALANCE,
        )
    }
}