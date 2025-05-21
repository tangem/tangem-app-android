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
 * `002` - First card scan error
 * `003` - Activation
 * `004` - Authorization API
 */
object VisaError : UniversalError {
    override val errorCode: Int = 104000000
}

enum class VisaCardScanError(
    override val errorCode: Int,
) : UniversalError {
    FailedToCreateDerivationPath(104002001),
    FailedToFindWallet(104002002),
    FailedToFindDerivedWalletKey(104002003),
    FailedToSignChallenge(104002004),
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
    VisaCardForApproval(104003011),
    CardIdNotMatched(104003011),
    FailedToSetPinCode(104003012),
}

sealed class VisaApiError(
    override val errorCode: Int,
) : UniversalError {
    // TODO codes may change
    data object Unspecified : VisaApiError(104100000)
    data object ProductInstanceNotFoundActivationRequired : VisaApiError(104100100)
    data object ProductInstanceIsBlocked : VisaApiError(104101000)
    data object ProductInstanceIsNotActivated : VisaApiError(104101100)
    data object ProductInstanceIsAlreadyActivated : VisaApiError(104101200)
    data object CustomerIsBlocked : VisaApiError(104102000)
    data object UnknownWithoutCode : VisaApiError(104101999)
    data class Unknown(override val errorCode: Int) : VisaApiError(errorCode)

    data object RefreshTokenExpired : VisaApiError(104004001)

    companion object {
        fun fromBackendError(backendErrorCode: Int): VisaApiError {
            val universalErrorCode = 104_000_000 + backendErrorCode
            return when (universalErrorCode) {
                104100000 -> Unspecified
                104100100 -> ProductInstanceNotFoundActivationRequired
                104101000 -> ProductInstanceIsBlocked
                104101100 -> ProductInstanceIsNotActivated
                104101200 -> ProductInstanceIsAlreadyActivated
                104102000 -> CustomerIsBlocked
                else -> Unknown(universalErrorCode)
            }
        }
    }
}