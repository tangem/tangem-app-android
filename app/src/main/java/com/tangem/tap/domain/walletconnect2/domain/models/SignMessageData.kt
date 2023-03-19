package com.tangem.tap.domain.walletconnect2.domain.models

@kotlinx.serialization.Serializable
data class SignMessageData(
    val address: String,
    val message: String,
)