package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.ensure
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.operations.TokenListFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Use case to toggle the sorting of token lists in account statuses to be sorted by balance.
 *
 * The use case ensures that the token list is not empty
 * and not in a loading state before performing the sorting operation.
 *
 * @property dispatchers Provides coroutine dispatchers for executing tasks.
 */
class ToggleTokenListSortingUseCaseV2(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Toggles the sorting of token lists in the provided [accountStatusList] to be sorted by balance.
     *
     * @param accountStatusList The list of account statuses containing token lists to be sorted.
     * @return Either a [TokenListSortingError] if an error occurs, or the updated [AccountStatusList]
     *         with token lists sorted by balance.
     */
    suspend operator fun invoke(
        accountStatusList: AccountStatusList,
    ): Either<TokenListSortingError, AccountStatusList> = eitherOn(dispatchers.default) {
        ensure(accountStatusList.flattenCurrencies().isNotEmpty()) {
            raise(TokenListSortingError.TokenListIsEmpty)
        }

        ensure(accountStatusList.totalFiatBalance !is TotalFiatBalance.Loading) {
            TokenListSortingError.TokenListIsLoading
        }

        accountStatusList.copy(
            accountStatuses = accountStatusList.accountStatuses.map { account ->
                if (account !is AccountStatus.CryptoPortfolio) return@map account

                account.copy(tokenList = account.tokenList.sortByBalance())
            },
            sortType = TokensSortType.BALANCE,
        )
    }

    private fun TokenList.sortByBalance(): TokenList {
        if (this is TokenList.Empty) return this

        return TokenListFactory.create(
            statuses = flattenCurrencies(),
            groupType = when (this) {
                is TokenList.GroupedByNetwork -> TokensGroupType.NETWORK
                is TokenList.Ungrouped -> TokensGroupType.NONE
                is TokenList.Empty -> {
                    return this
                }
            },
            sortType = TokensSortType.BALANCE,
        )
    }
}