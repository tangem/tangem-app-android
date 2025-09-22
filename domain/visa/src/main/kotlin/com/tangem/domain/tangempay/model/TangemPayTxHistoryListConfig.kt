package com.tangem.domain.tangempay.model

import com.tangem.domain.models.wallet.UserWalletId

data class TangemPayTxHistoryListConfig(val userWalletId: UserWalletId, val refresh: Boolean)