package com.tangem.domain.account.utils

import com.tangem.domain.models.account.*
import com.tangem.domain.models.wallet.UserWalletId
import kotlin.random.Random

fun createAccounts(userWalletId: UserWalletId, count: Int): Set<Account.CryptoPortfolio> {
    return buildSet {
        add(Account.CryptoPortfolio.createMainAccount(userWalletId))

        repeat(count - 1) {
            val account = createAccount(
                userWalletId = userWalletId,
                name = "Test Account ${it + 1}",
                derivationIndex = it + 1,
            )

            add(account)
        }
    }
}

fun createAccount(
    userWalletId: UserWalletId,
    name: String = "Test Account",
    icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
    derivationIndex: Int = Random.nextInt(1, 21),
): Account.CryptoPortfolio {
    val derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!

    return Account.CryptoPortfolio(
        accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
        accountName = AccountName(name).getOrNull()!!,
        icon = icon,
        derivationIndex = derivationIndex,
        cryptoCurrencies = emptySet(),
    )
}