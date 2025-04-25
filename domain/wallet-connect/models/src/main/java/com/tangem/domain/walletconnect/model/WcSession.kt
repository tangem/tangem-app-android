package com.tangem.domain.walletconnect.model

import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.wallets.models.UserWalletId

data class WcSession(
    val userWalletId: UserWalletId,
    val sdkModel: WcSdkSession,
)