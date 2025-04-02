package com.tangem.domain.walletconnect.model

import com.squareup.moshi.JsonClass
import com.tangem.domain.wallets.models.UserWalletId

@JsonClass(generateAdapter = true)
data class WcSessionDTO(
    val topic: String,
    val walletId: UserWalletId,
)