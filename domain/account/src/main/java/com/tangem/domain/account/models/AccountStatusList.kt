package com.tangem.domain.account.models

import arrow.core.Either
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Represents a list of account statuses associated with a user wallet
 *
 * @property userWalletId     the user wallet id to which the account statuses belong
 * @property accountStatuses  a set of account statuses associated with the user wallet
 * @property totalAccounts    the total number of crypto accounts (including archived ones)
 * @property totalJointAccounts the total number of joint accounts (including archived ones)
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
    val totalJointAccounts: Int = 0,
    val totalFiatBalance: TotalFiatBalance,
    val sortType: TokensSortType,
    val groupType: TokensGroupType,
) {

    /** A joint account is never the main one — the main account is always the wallet's own */
    val mainAccount: AccountStatus.Personal
        get() = accountStatuses
            .filterIsInstance<AccountStatus.Personal>()
            .first { accountStatus -> accountStatus.account.isMainAccount }

    fun flattenCurrencies(): List<CryptoCurrencyStatus> = accountStatuses
        .filterCryptoPortfolio()
        .map { accountStatus -> accountStatus.flattenCurrencies() }
        .flatten()

    /** Currencies of the wallet's own accounts only — for the flows that spend from the wallet's keys */
    fun flattenPersonalCurrencies(): List<CryptoCurrencyStatus> = accountStatuses
        .filterIsInstance<AccountStatus.Personal>()
        .map { accountStatus -> accountStatus.flattenCurrencies() }
        .flatten()

    fun toAccountList(): Either<AccountList.Error, AccountList> {
        return AccountList(
            userWalletId = userWalletId,
            accounts = accountStatuses.map(AccountStatus::account),
            totalAccounts = totalAccounts,
            totalArchivedAccounts = totalArchivedAccounts,
            totalJointAccounts = totalJointAccounts,
            sortType = sortType,
            groupType = groupType,
        )
    }
}

fun AccountStatusList.hasMultiCurrencyAccount(): Boolean = accountStatuses.any { status ->
    when (status) {
        is AccountStatus.Personal -> status.tokenList.flattenCurrencies().size > 1
        // The token list of a joint account stays empty until it is active; its currencies are known all along
        is AccountStatus.Joint -> status.account.cryptoCurrencies.size > 1
        is AccountStatus.Payment -> false
        is AccountStatus.Money -> false
        is AccountStatus.Prediction -> false
        is AccountStatus.Collectibles -> false
    }
}