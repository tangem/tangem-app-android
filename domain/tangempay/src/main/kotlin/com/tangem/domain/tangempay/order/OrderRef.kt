package com.tangem.domain.tangempay.order

import com.tangem.domain.models.wallet.UserWalletId

data class OrderRef(
    val userWalletId: UserWalletId,
    val id: String,
)