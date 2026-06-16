package com.tangem.domain.transaction.error

sealed class SignHashesError {

    /** The wallet has no usable signing key (e.g. it is locked or has no secp256k1 key). */
    data object NoSigningKey : SignHashesError()

    /** The signing session failed or was canceled by the user. */
    data class SigningFailed(val message: String) : SignHashesError()
}