package com.tangem.domain.walletconnect.model.sdkcopy

/**
 * copy of [com.reown.walletkit.client.Wallet.Model.SessionProposal]
 */
data class WcSdkSessionProposal(
    val name: String,
    val description: String,
    val url: String,
    val proposerPublicKey: String,
)