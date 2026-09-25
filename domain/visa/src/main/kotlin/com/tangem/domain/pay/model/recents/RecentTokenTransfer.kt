package com.tangem.domain.pay.model.recents

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency

/** A token of a crypto portfolio account the payment account has recently exchanged with. */
data class RecentTokenTransfer(
    val account: Account.CryptoPortfolio,
    val currency: CryptoCurrency,
)