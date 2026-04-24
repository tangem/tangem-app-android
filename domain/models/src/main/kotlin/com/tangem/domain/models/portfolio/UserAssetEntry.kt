package com.tangem.domain.models.portfolio

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId

data class UserAssetEntry(
    val userWalletId: UserWalletId,
    val userWalletName: String,
    val accountId: AccountId,
    val accountName: AccountName,
    val accountIcon: CryptoPortfolioIcon,
    val currencyStatus: CryptoCurrencyStatus,
)