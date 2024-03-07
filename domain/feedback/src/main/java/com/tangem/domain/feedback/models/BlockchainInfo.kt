package com.tangem.domain.feedback.models

data class BlockchainInfo(
    val blockchain: String,
    val derivationPath: String,
    val outputsCount: String?,
    val host: String,
    val addresses: String,
    val explorerLink: String,
    val tokens: List<TokenInfo>,
) {

    data class TokenInfo(
        val id: String?,
        val name: String,
        val contractAddress: String,
    )
}