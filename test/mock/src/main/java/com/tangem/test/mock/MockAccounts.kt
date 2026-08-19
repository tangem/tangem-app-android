package com.tangem.test.mock

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
[REDACTED_AUTHOR]
 */
object MockAccounts {

    val userWalletId = UserWalletId("011")

    val onlyMainAccount = createAccountList(activeAccounts = 1)

    val fullAccountList = createAccountList(activeAccounts = 20)

    fun createAccountList(
        activeAccounts: Int,
        totalAccounts: Int = activeAccounts,
        totalArchivedAccounts: Int = 0,
        userWalletId: UserWalletId = this.userWalletId,
    ): AccountList {
        return AccountList(
            userWalletId = userWalletId,
            accounts = createAccounts(count = activeAccounts, userWalletId = userWalletId),
            totalAccounts = totalAccounts,
            totalArchivedAccounts = totalArchivedAccounts,
        ).getOrNull()!!
    }

    fun createAccounts(count: Int, userWalletId: UserWalletId = this.userWalletId): List<Account.CryptoPortfolio> {
        return buildList {
            add(Account.CryptoPortfolio.createMainAccount(userWalletId))

            repeat(count - 1) {
                val account = createAccount(derivationIndex = it + 1, userWalletId = userWalletId)

                add(account)
            }
        }
    }

    fun createJointAccount(
        derivationIndex: Int = 0,
        name: String = "Joint #$derivationIndex",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        cryptoCurrencies: List<CryptoCurrency> = emptyList(),
        userWalletId: UserWalletId = this.userWalletId,
    ): Account.Joint {
        val backendId = derivationIndex.toString(radix = 16).padStart(length = 64, padChar = '0')

        return Account.Joint(
            accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = backendId).getOrNull()!!,
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!,
            cryptoCurrencies = cryptoCurrencies,
        )
    }

    fun createAccount(
        derivationIndex: Int,
        name: String = "Account #$derivationIndex",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        cryptoCurrencies: List<CryptoCurrency> = emptyList(),
        userWalletId: UserWalletId = this.userWalletId,
    ): Account.CryptoPortfolio {
        val derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!

        return Account.CryptoPortfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            derivationIndex = derivationIndex,
            cryptoCurrencies = cryptoCurrencies,
        )
    }
}