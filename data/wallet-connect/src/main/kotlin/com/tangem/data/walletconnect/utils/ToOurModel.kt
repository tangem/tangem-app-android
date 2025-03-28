package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest.JSONRPCRequest

internal fun Wallet.Model.Session.toOurModel(): WcSdkSession = WcSdkSession(
    topic = topic,
)

internal fun Wallet.Model.SessionRequest.toOurModel(): WcSdkSessionRequest = WcSdkSessionRequest(
    topic = this.topic,
    chainId = this.chainId,
    request = this.request.toOurModel(),
)

internal fun Wallet.Model.SessionRequest.JSONRPCRequest.toOurModel(): JSONRPCRequest = JSONRPCRequest(
    id = this.id,
    method = this.method,
    params = this.params,
)