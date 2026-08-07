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
 * `005` - Plastic card order
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
    data object Unspecified : VisaApiError(104110205)
    data object ProductInstanceNotFoundActivationRequired : VisaApiError(104110206)
    data object ProductInstanceIsBlocked : VisaApiError(104110209)
    data object ProductInstanceIsNotActivated : VisaApiError(104110208)
    data object ProductInstanceIsAlreadyActivated : VisaApiError(104110207)
    data object CustomerIsBlocked : VisaApiError(104110210)
    data object CardIssueInsufficientBalance : VisaApiError(104140116)
    data object UnknownWithoutCode : VisaApiError(104110999)
    data class Unknown(override val errorCode: Int) : VisaApiError(errorCode)

    fun isUnknown() = this is UnknownWithoutCode || this is Unknown

    data object RefreshTokenExpired : VisaApiError(104004001)
    data object NotFound : VisaApiError(104004002)
    data object WithdrawalDataError : VisaApiError(104004003)
    data object SignWithdrawError : VisaApiError(104004004)
    data object WithdrawError : VisaApiError(104004005)
    data object ServerUnavailable : VisaApiError(104004006)
    data object CustomerIdUnavailable : VisaApiError(104004007)
    data object OrderNotFound : VisaApiError(104004008)

    data object InsufficientFunds : VisaApiError(104005001)
    data object InvalidShippingAddress : VisaApiError(104005002)
    data object CountryNotSupported : VisaApiError(104005003)
    data object OfferNotAvailable : VisaApiError(104005004)
    data object AlreadyHasActiveOrder : VisaApiError(104005005)
    data object PlasticNotAvailable : VisaApiError(104005006)

    companion object {

        fun fromBackendErrorName(backendErrorName: String?): VisaApiError? = when (backendErrorName) {
            "INSUFFICIENT_FUNDS" -> InsufficientFunds
            "INVALID_SHIPPING_ADDRESS" -> InvalidShippingAddress
            "COUNTRY_NOT_SUPPORTED" -> CountryNotSupported
            "OFFER_NOT_AVAILABLE" -> OfferNotAvailable
            "ALREADY_HAS_ACTIVE_ORDER" -> AlreadyHasActiveOrder
            "PLASTIC_NOT_AVAILABLE" -> PlasticNotAvailable
            else -> null
        }

        fun fromBackendError(backendErrorCode: Int): VisaApiError {
            val universalErrorCode = 104_000_000 + backendErrorCode
            return when (universalErrorCode) {
                Unspecified.errorCode -> Unspecified
                ProductInstanceNotFoundActivationRequired.errorCode -> ProductInstanceNotFoundActivationRequired
                ProductInstanceIsBlocked.errorCode -> ProductInstanceIsBlocked
                ProductInstanceIsNotActivated.errorCode -> ProductInstanceIsNotActivated
                ProductInstanceIsAlreadyActivated.errorCode -> ProductInstanceIsAlreadyActivated
                CustomerIsBlocked.errorCode -> CustomerIsBlocked
                CardIssueInsufficientBalance.errorCode -> CardIssueInsufficientBalance
                else -> Unknown(universalErrorCode)
            }
        }
    }
}