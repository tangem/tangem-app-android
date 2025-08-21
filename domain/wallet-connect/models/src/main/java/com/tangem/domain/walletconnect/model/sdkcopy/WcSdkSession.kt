package com.tangem.domain.walletconnect.model.sdkcopy

/**
 * copy of [com.reown.walletkit.client.Wallet.Model.Session]
 */
data class WcSdkSession(
    val topic: String,
    val appMetaData: WcAppMetaData,
    val namespaces: Map<String, Session>,
) {
    data class Session(
        val chains: List<String>,
        val accounts: List<String>,
        val methods: List<String>,
        val events: List<String>,
    )
}