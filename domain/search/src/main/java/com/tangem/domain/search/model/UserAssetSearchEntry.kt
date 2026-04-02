package com.tangem.domain.search.model

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId

data class UserAssetSearchEntry(
    val userWalletId: UserWalletId,
    val userWalletName: String,
    val accountId: AccountId,
    val accountName: AccountName,
    val currencyStatus: CryptoCurrencyStatus,
)