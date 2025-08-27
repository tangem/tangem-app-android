package com.tangem.data.account.converter

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
    val accountsListCF: AccountListConverter.Factory,
    val getWalletAccountsResponseCF: GetWalletAccountsResponseConverter.Factory,
    val cryptoPortfolioCF: CryptoPortfolioConverter.Factory,
)