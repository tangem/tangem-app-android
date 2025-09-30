package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.operations.TokenListFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class ToggleTokenListSortingUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(tokenList: TokenList): Either<TokenListSortingError, TokenList> {
        return withContext(dispatchers.default) {
            either {
                ensure(tokenList.totalFiatBalance !is TotalFiatBalance.Loading) {
                    TokenListSortingError.TokenListIsLoading
                }

                TokenListFactory.create(
                    statuses = tokenList.flattenCurrencies(),
                    groupType = when (tokenList) {
                        is TokenList.GroupedByNetwork -> TokensGroupType.NETWORK
                        is TokenList.Ungrouped -> TokensGroupType.NONE
                        is TokenList.Empty -> raise(TokenListSortingError.TokenListIsEmpty)
                    },
                    sortType = TokensSortType.BALANCE,
                )
            }
        }
    }
}