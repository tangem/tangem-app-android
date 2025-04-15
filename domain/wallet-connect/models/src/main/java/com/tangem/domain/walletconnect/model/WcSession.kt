package com.tangem.domain.walletconnect.model

import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.wallets.models.UserWallet

data class WcSession(
    val wallet: UserWallet,
    val sdkModel: WcSdkSession,
)