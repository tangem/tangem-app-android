package com.tangem.domain.visa.model

enum class VisaActivationError(val message: String) {
    BlockedForActivation("Card is blocked for activation"),
    InvalidActivationState("Invalid activation state"),
}
