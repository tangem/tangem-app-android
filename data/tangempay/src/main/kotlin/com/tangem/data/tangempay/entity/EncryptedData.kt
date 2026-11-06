package com.tangem.data.tangempay.entity

internal data class EncryptedData(
    val encryptedBase64: String,
    val ivBase64: String,
)