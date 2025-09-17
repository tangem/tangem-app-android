package com.tangem.domain.account.models

import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.serialization.Serializable

/**
 * Represents a list of account statuses associated with a user wallet
 *
 * @property userWallet      the user wallet to which the account statuses belong
 * @property accountStatuses a set of account statuses associated with the user wallet
 * @property totalAccounts   the total number of accounts
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountStatusList(
    val userWallet: UserWallet,
    val accountStatuses: Set<AccountStatus>,
    val totalAccounts: Int,
    val totalFiatBalance: TotalFiatBalance = TotalFiatBalance.Failed, // todo account required for total wallet balance
) {

    // todo account copy from [AccountList] ok?
    val mainAccount: AccountStatus
        get() = accountStatuses.first { accountStatus ->
            when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> accountStatus.account.isMainAccount
            }
        }
}