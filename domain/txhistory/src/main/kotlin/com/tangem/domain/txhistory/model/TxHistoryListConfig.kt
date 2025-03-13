package com.tangem.domain.txhistory.model

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

data class TxHistoryListConfig(val userWalletId: UserWalletId, val currency: CryptoCurrency, val refresh: Boolean)