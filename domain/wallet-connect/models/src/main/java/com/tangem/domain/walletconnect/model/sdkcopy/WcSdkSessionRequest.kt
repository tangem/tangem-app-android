package com.tangem.domain.walletconnect.model.sdkcopy

import kotlinx.serialization.Serializable

/**
 * copy of [com.reown.walletkit.client.Wallet.Model.SessionRequest]
 */
@Serializable
data class WcSdkSessionRequest(
    val topic: String,
    val chainId: String?,
    val dAppMetaData: WcAppMetaData,
    val request: JSONRPCRequest,
) {

    @Serializable
    data class JSONRPCRequest(
        val id: Long,
        val method: String,
        val params: String,
    )
}