@file:Suppress("MagicNumber")

package com.tangem.domain.visa.error

import com.tangem.core.error.UniversalError

/**
 * Each error code must follow this format: xxxyyyzzz where
 * xxx - Feature code
 * yyy - Subsystem code
 * zzz - Specific error code
 * If you need to add new subsystem add it to list below incrementing last code.
 * `Subsystems`:
 * `001` - Common API
 * `002` - First card scan error
 * `003` - Activation
 * `004` - Authorization API
 */
object VisaError : UniversalError {
    override val errorCode: Int = 104000000
}

object VisaAPIError : UniversalError {
    override val errorCode: Int = 104001000
}

enum class VisaCardScanError(
    override val errorCode: Int,
) : UniversalError {
    FailedToCreateDerivationPath(104002001),
    FailedToFindWallet(104002002),
    FailedToFindDerivedWalletKey(104002003),
}

enum class VisaActivationError(
    override val errorCode: Int,
) : UniversalError {
    BlockedForActivation(104003001),
    InvalidActivationState(104003002),
    WrongCard(104003003),
    WrongRemoteState(104003004),
    MissingWallet(104003005),
    MissingRootOTP(104003006),
    FailedToCreateAddress(104003007),
    AddressNotMatched(104003008),
    InconsistentRemoteState(104003009),
    FailedRemoteState(104003010),
}

object VisaAuthorizationAPIError : UniversalError {
    override val errorCode: Int = 104004000
}