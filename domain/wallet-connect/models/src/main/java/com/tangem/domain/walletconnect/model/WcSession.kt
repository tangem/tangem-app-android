package com.tangem.domain.walletconnect.model

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.wallets.models.UserWallet

data class WcSession(
    val wallet: UserWallet,
    val sdkModel: WcSdkSession,
    val securityStatus: CheckDAppResult,
)