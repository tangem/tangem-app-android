package com.tangem.data.account.converter

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Container for converter factories related to accounts.
 *
 * @property accountsListCF              factory for creating an account list converter
 * @property getWalletAccountsResponseCF factory for creating a wallet accounts response converter
 * @property cryptoPortfolioCF           factory for creating a crypto portfolio converter
 *
 * @constructor Creates an instance of the container with injected factories.
 *
[REDACTED_AUTHOR]
 */
internal class AccountConverterFactoryContainer @Inject constructor(
    val getWalletAccountsResponseCF: GetWalletAccountsResponseConverter.Factory,
    private val accountsListCF: AccountListConverter.Factory,
    private val cryptoPortfolioCF: CryptoPortfolioConverter.Factory,
    private val userWalletsStore: UserWalletsStore,
) {

    fun createAccountListConverter(userWalletId: UserWalletId): AccountListConverter {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)

        return accountsListCF.create(userWallet)
    }

    fun createCryptoPortfolioConverter(userWalletId: UserWalletId): CryptoPortfolioConverter {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)

        return cryptoPortfolioCF.create(userWallet)
    }
}