package com.tangem.domain.feedback.models

data class BlockchainInfo(
    val blockchain: String,
    val derivationPath: String,
    val outputsCount: String?,
    val host: String,
    val addresses: Addresses,
    val explorerLinks: Addresses,
    val tokens: List<TokenInfo>,
) {

    sealed class Addresses {

        data class Single(val value: String) : Addresses()

        data class Multiple(val values: List<AddressInfo>) : Addresses() {

            data class AddressInfo(val type: String, val value: String)
        }
    }

    data class TokenInfo(
        val id: String?,
        val name: String,
        val contractAddress: String,
        val decimals: String,
    )
}