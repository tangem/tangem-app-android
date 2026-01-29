package com.tangem.features.onramp.swap.entity

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus

internal data class AccountAvailabilityUM(
    val account: Account.Crypto,
    val currencyList: List<AccountCurrencyUM>,
)

internal data class AccountCurrencyUM(
    val isAvailable: Boolean,
    val cryptoCurrencyStatus: CryptoCurrencyStatus,
)