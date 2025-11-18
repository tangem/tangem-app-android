package com.tangem.domain.account.models

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
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountStatusList(
    val userWalletId: UserWalletId,
    val accountStatuses: Set<AccountStatus>,
    val totalAccounts: Int,
    val totalFiatBalance: TotalFiatBalance,
) {

    val mainAccount: AccountStatus
        get() = accountStatuses.first { accountStatus ->
            when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> accountStatus.account.isMainAccount
            }
        }

    fun flattenCurrencies(): List<CryptoCurrencyStatus> = accountStatuses
        .map { accountStatus -> accountStatus.flattenCurrencies() }
        .flatten()
}