package com.tangem.domain.transaction.error

sealed class SignHashesError {

    /** The wallet has no usable signing key (e.g. it is locked or has no secp256k1 key). */
    data object NoSigningKey : SignHashesError()

    /** The user dismissed the signing session — not a failure, the caller decides whether to retry. */
    data object Cancelled : SignHashesError()

    /** The signing session failed. */
    data class SigningFailed(val message: String) : SignHashesError()
}