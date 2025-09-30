package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.operations.TokenListFactory
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
        validate(tokenList)

        return TokenListFactory.createGroupedByNetwork(tokenList)
    }

    private fun Raise<TokenListSortingError>.ungroupTokens(tokenList: TokenList.GroupedByNetwork): TokenList.Ungrouped {
        validate(tokenList)

        return TokenListFactory.createUngrouped(tokenList)
    }

    private fun Raise<TokenListSortingError>.validate(tokenList: TokenList) {
        ensure(tokenList.totalFiatBalance !is TotalFiatBalance.Loading) {
            TokenListSortingError.TokenListIsLoading
        }

        ensure(tokenList.flattenCurrencies().isNotEmpty()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }
}