package com.tangem.domain.walletconnect.model

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.wallet.UserWalletId

@JsonClass(generateAdapter = true)
data class WcSessionDTO(
    val topic: String,
    val walletId: UserWalletId,
    val securityStatus: CheckDAppResult = CheckDAppResult.FAILED_TO_VERIFY,
    val connectingTime: Long? = null,
)