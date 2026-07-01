package com.tangem.domain.transaction.error

sealed class VerifyMessagesError {

    /** The wallet has no usable signing key (e.g. it is locked or has no secp256k1 key). */
    data object NoSigningKey : VerifyMessagesError()
}