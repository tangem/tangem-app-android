package com.tangem.data.pay.entity

internal data class EncryptedData(
    val encryptedBase64: String,
    val ivBase64: String,
)