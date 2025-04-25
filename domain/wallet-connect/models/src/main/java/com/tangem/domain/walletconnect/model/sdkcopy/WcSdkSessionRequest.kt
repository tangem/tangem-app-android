package com.tangem.domain.walletconnect.model.sdkcopy

/**
 * copy of [com.reown.walletkit.client.Wallet.Model.SessionRequest]
 */
data class WcSdkSessionRequest(
    val topic: String,
    val chainId: String?,
    val request: JSONRPCRequest,
) {

    data class JSONRPCRequest(
        val id: Long,
        val method: String,
        val params: String,
    )
}