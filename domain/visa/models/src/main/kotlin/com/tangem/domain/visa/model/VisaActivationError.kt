package com.tangem.domain.visa.model

enum class VisaActivationError(val message: String) {
    BlockedForActivation("Card is blocked for activation"),
    InvalidActivationState("Invalid activation state"),
    WrongCard("Wrong card tapped"),
    WrongRemoteState("Wrong remote state"),
    MissingWallet("Missing wallet"),
    MissingRootOTP("Missing root OTP"),
}