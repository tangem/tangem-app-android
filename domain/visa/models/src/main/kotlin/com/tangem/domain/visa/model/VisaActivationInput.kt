package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaActivationInput(
    val cardId: String,
    val cardPublicKey: String,
    val isAccessCodeSet: Boolean,
)