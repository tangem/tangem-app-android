package com.tangem.domain.walletconnect.model

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.models.wallet.UserWallet

data class WcSession(
    val wallet: UserWallet,
    val networks: Set<Network>,
    val sdkModel: WcSdkSession,
    val securityStatus: CheckDAppResult,
    val connectingTime: Long?,
    val showWalletInfo: Boolean,
)