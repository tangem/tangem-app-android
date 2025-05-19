package com.tangem.domain.visa.model

data class VisaEncryptedPinCode(
    val activationOrderId: String,
    val sessionId: String,
    val iv: String,
    val encryptedPin: String,
)