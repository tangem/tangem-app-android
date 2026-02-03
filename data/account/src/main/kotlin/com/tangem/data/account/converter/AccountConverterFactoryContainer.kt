package com.tangem.data.account.converter

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Container for converter factories related to accounts.
 *
 * @property getWalletAccountsResponseCF factory for creating a wallet accounts response converter
 * @property accountsListCF              factory for creating an account list converter
 * @property cryptoPortfolioCF           factory for creating a crypto portfolio converter
 * @property userWalletsListRepository   repository for getting user wallets
 *
 * @constructor Creates an instance of the container with injected factories.
 *
[REDACTED_AUTHOR]
 */
internal class AccountConverterFactoryContainer @Inject constructor(
    private val getWalletAccountsResponseCF: GetWalletAccountsResponseConverter.Factory,
    private val accountsListCF: AccountListConverter.Factory,
    private val cryptoPortfolioCF: CryptoPortfolioConverter.Factory,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    fun createWalletAccountsResponseConverter(userWalletId: UserWalletId): GetWalletAccountsResponseConverter {
        val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)

        return getWalletAccountsResponseCF.create(userWallet)
    }

    fun createAccountListConverter(userWalletId: UserWalletId): AccountListConverter {
        val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)

        return accountsListCF.create(userWallet)
    }

    fun createCryptoPortfolioConverter(userWalletId: UserWalletId): CryptoPortfolioConverter {
        val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)

        return cryptoPortfolioCF.create(userWallet)
    }
}