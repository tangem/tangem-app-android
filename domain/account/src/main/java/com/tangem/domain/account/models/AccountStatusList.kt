package com.tangem.domain.account.models

import arrow.core.Either
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Represents a list of account statuses associated with a user wallet
 *
 * @property userWalletId     the user wallet id to which the account statuses belong
 * @property accountStatuses  a set of account statuses associated with the user wallet
 * @property totalAccounts    the total number of accounts (including archived ones)
 * @property totalFiatBalance the total fiat balance across all accounts
 * @property sortType      the sorting type applied to the accounts
 * @property groupType     the grouping type applied to the accounts
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountStatusList(
    val userWalletId: UserWalletId,
    val accountStatuses: List<AccountStatus>,
    val totalAccounts: Int,
    val totalArchivedAccounts: Int,
    val totalFiatBalance: TotalFiatBalance,
    val sortType: TokensSortType,
    val groupType: TokensGroupType,
) {

    val mainAccount: AccountStatus.Crypto.Portfolio
        get() = accountStatuses
            .filterIsInstance<AccountStatus.Crypto>()
            .first { accountStatus ->
                when (accountStatus) {
                    is AccountStatus.Crypto.Portfolio -> accountStatus.account.isMainAccount
                }
            } as AccountStatus.Crypto.Portfolio

    fun flattenCurrencies(): List<CryptoCurrencyStatus> = accountStatuses
        .filterIsInstance<AccountStatus.Crypto.Portfolio>()
        .map { accountStatus -> accountStatus.flattenCurrencies() }
        .flatten()

    fun toAccountList(): Either<AccountList.Error, AccountList> {
        return AccountList(
            userWalletId = userWalletId,
            accounts = accountStatuses.map(AccountStatus::account),
            totalAccounts = totalAccounts,
            totalArchivedAccounts = totalArchivedAccounts,
            sortType = sortType,
            groupType = groupType,
        )
    }
}