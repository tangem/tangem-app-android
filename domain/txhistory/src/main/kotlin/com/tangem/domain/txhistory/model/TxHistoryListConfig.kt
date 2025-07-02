package com.tangem.domain.txhistory.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

data class TxHistoryListConfig(val userWalletId: UserWalletId, val currency: CryptoCurrency, val refresh: Boolean)