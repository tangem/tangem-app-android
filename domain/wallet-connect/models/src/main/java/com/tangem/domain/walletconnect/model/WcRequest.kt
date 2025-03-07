package com.tangem.domain.walletconnect.model

import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

data class WcRequest<M : WcMethod>(
    val rawSdkRequest: WcSdkSessionRequest,
    val session: WcSession,
    val method: M,
)