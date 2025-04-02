package com.tangem.domain.walletconnect.model.sdkcopy

/**
 * copy of [com.reown.walletkit.client.Wallet.Model.Session]
 */
data class WcSdkSession(
    val topic: String,
    val appMetaData: WcAppMetaData,
)