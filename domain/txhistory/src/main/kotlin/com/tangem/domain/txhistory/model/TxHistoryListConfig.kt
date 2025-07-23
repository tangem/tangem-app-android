package com.tangem.domain.txhistory.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

data class TxHistoryListConfig(val userWalletId: UserWalletId, val currency: CryptoCurrency, val refresh: Boolean)