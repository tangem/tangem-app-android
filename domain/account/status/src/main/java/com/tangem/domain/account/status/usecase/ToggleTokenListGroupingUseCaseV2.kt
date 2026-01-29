package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.ensure
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.operations.TokenListFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Use case to toggle the grouping of token lists in account statuses.
 *
 * If the token list is currently ungrouped, it will be grouped by network,
 * and vice versa. The use case ensures that the token list is not empty
 * and not in a loading state before performing the toggle operation.
 *
 * @property dispatchers Provides coroutine dispatchers for executing tasks.
 */
class ToggleTokenListGroupingUseCaseV2(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Toggles the grouping of token lists in the provided [accountStatusList].
     *
     * @param accountStatusList The list of account statuses containing token lists to be toggled.
     * @return Either a [TokenListSortingError] if an error occurs, or the updated [AccountStatusList]
     *         with toggled token list grouping.
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

        val updatedAccountList = accountStatusList.accountStatuses.map { account ->
            if (account !is AccountStatus.Crypto.Portfolio) return@map account

            account.copy(tokenList = account.tokenList.reverseGroupType())
        }
        val updatedCryptoAccount =
            updatedAccountList.firstOrNull { it is AccountStatus.Crypto } as? AccountStatus.Crypto

        accountStatusList.copy(
            accountStatuses = updatedAccountList,
            groupType = when (updatedCryptoAccount?.getCryptoTokenList()) {
                is TokenList.GroupedByNetwork -> TokensGroupType.NETWORK
                is TokenList.Ungrouped -> TokensGroupType.NONE
                else -> accountStatusList.groupType
            },
        )
    }

    private fun TokenList.reverseGroupType(): TokenList {
        return when (this) {
            is TokenList.Ungrouped -> TokenListFactory.createGroupedByNetwork(this)
            is TokenList.GroupedByNetwork -> TokenListFactory.createUngrouped(this)
            is TokenList.Empty -> this
        }
    }
}