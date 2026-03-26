package com.tangem.domain.payment.models.auth

import com.tangem.core.error.UniversalError

@Suppress("MagicNumber")
sealed class PaymentAuthApiError(
    override val errorCode: Int,
) : UniversalError {
    data object Unspecified : PaymentAuthApiError(104110205)
    data object ProductInstanceNotFoundActivationRequired : PaymentAuthApiError(104110206)
    data object ProductInstanceIsBlocked : PaymentAuthApiError(104110209)
    data object ProductInstanceIsNotActivated : PaymentAuthApiError(104110208)
    data object ProductInstanceIsAlreadyActivated : PaymentAuthApiError(104110207)
    data object CustomerIsBlocked : PaymentAuthApiError(104110210)
    data object UnknownWithoutCode : PaymentAuthApiError(104110999)

    data object FailedToSignChallenge : PaymentAuthApiError(104002004)

    data object MissingWallet : PaymentAuthApiError(104003005)

    data object FailedRemoteState : PaymentAuthApiError(104003010)

    data class Unknown(override val errorCode: Int) : PaymentAuthApiError(errorCode)

    companion object {
        fun fromBackendError(backendErrorCode: Int): PaymentAuthApiError {
            val universalErrorCode = 104_000_000 + backendErrorCode
            return when (universalErrorCode) {
                Unspecified.errorCode -> Unspecified
                ProductInstanceNotFoundActivationRequired.errorCode -> ProductInstanceNotFoundActivationRequired
                ProductInstanceIsBlocked.errorCode -> ProductInstanceIsBlocked
                ProductInstanceIsNotActivated.errorCode -> ProductInstanceIsNotActivated
                ProductInstanceIsAlreadyActivated.errorCode -> ProductInstanceIsAlreadyActivated
                CustomerIsBlocked.errorCode -> CustomerIsBlocked
                else -> Unknown(universalErrorCode)
            }
        }
    }
}