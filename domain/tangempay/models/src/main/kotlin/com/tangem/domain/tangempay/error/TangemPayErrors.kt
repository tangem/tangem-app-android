@file:Suppress("MagicNumber")

package com.tangem.domain.tangempay.error

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
enum class TangemPayCardScanError(
    override val errorCode: Int,
) : UniversalError {
    FailedToSignChallenge(104002004),
}

enum class TangemPayActivationError(
    override val errorCode: Int,
) : UniversalError {
    MissingWallet(104003005),
    FailedRemoteState(104003010),
    PublicKeyIsEmpty(104003013),
}

sealed class TangemPayApiError(
    override val errorCode: Int,
) : UniversalError {
    data object Unspecified : TangemPayApiError(104110205)
    data object ProductInstanceNotFoundActivationRequired : TangemPayApiError(104110206)
    data object ProductInstanceIsAlreadyActivated : TangemPayApiError(104110207)
    data object CustomerIsBlocked : TangemPayApiError(104110210)
    data object CardIssueActiveOrderExists : TangemPayApiError(104140114)
    data object CardIssueOfferNotAvailable : TangemPayApiError(104140115)
    data object CardIssueInsufficientBalance : TangemPayApiError(104140116)
    data object CardIssueInvalidShippingAddress : TangemPayApiError(104140126)
    data object CardIssueInvalidEmbossName : TangemPayApiError(104140144)
    data object UnknownWithoutCode : TangemPayApiError(104110999)
    data class Unknown(override val errorCode: Int) : TangemPayApiError(errorCode)

    fun isRetryable() = this == ServerUnavailable || this == UnknownWithoutCode

    fun isOutcomeUnknown() = this == ServerUnavailable || this == UnknownWithoutCode || this == RefreshTokenExpired

    data object RefreshTokenExpired : TangemPayApiError(104004001)
    data object NotFound : TangemPayApiError(104004002)
    data object WithdrawalDataError : TangemPayApiError(104004003)
    data object SignWithdrawError : TangemPayApiError(104004004)
    data object WithdrawError : TangemPayApiError(104004005)
    data object ServerUnavailable : TangemPayApiError(104004006)
    data object CustomerIdUnavailable : TangemPayApiError(104004007)
    data object OrderNotFound : TangemPayApiError(104004008)

    data object CardCloseLastCard : TangemPayApiError(104140120)

    data object CardActivationInvalidCardData : TangemPayApiError(104140127)
    data object CardActivationCardNotPhysical : TangemPayApiError(104140128)
    data object CardActivationCardAlreadyActive : TangemPayApiError(104140129)
    data object CardActivationCardNotReadyForActivation : TangemPayApiError(104140130)
    data object CardActivationActiveOrderExists : TangemPayApiError(104140131)

    data object CardReissuePlasticInvalidSourceCard : TangemPayApiError(104140132)
    data object CardReissuePlasticActiveOrderExists : TangemPayApiError(104140133)
    data object CardReissuePlasticInsufficientBalance : TangemPayApiError(104140134)
    data object CardReissuePlasticNotAvailable : TangemPayApiError(104140135)
    data object CardReissuePlasticInvalidShippingAddress : TangemPayApiError(104140136)
    data object CardReissuePlasticInvalidEmbossName : TangemPayApiError(104140143)

    data object BankOfframpUnsupportedConfiguration : TangemPayApiError(104140141)
    data object BankOfframpQuoteExpired : TangemPayApiError(104140164)
    data object BankOfframpRegionUnsupported : TangemPayApiError(104140165)
    data object BankOfframpVerificationIncomplete : TangemPayApiError(104140166)
    data object BankOfframpConflict : TangemPayApiError(104140167)
    data object BankOfframpAmountOutOfLimits : TangemPayApiError(104140168)
    data object BankOfframpRejected : TangemPayApiError(104140169)

    data object WaitlistInvalidEmail : TangemPayApiError(-1)
    data object AccountIssueActiveOrderExists : TangemPayApiError(-1)

    companion object {
        private const val WAITLIST_INVALID_EMAIL_NAME = "WaitlistInvalidEmailException"
        private const val ACCOUNT_ISSUE_ACTIVE_ORDER_EXISTS_NAME = "AccountIssueActiveOrderExistsException"

        fun fromBackendName(backendErrorName: String?): TangemPayApiError? = when (backendErrorName) {
            WAITLIST_INVALID_EMAIL_NAME -> WaitlistInvalidEmail
            ACCOUNT_ISSUE_ACTIVE_ORDER_EXISTS_NAME -> AccountIssueActiveOrderExists
            else -> null
        }

        @Suppress("CyclomaticComplexMethod")
        fun fromBackendError(backendErrorCode: Int): TangemPayApiError {
            val universalErrorCode = 104_000_000 + backendErrorCode
            return when (universalErrorCode) {
                Unspecified.errorCode -> Unspecified
                ProductInstanceNotFoundActivationRequired.errorCode -> ProductInstanceNotFoundActivationRequired
                ProductInstanceIsAlreadyActivated.errorCode -> ProductInstanceIsAlreadyActivated
                CustomerIsBlocked.errorCode -> CustomerIsBlocked
                CardIssueActiveOrderExists.errorCode -> CardIssueActiveOrderExists
                CardIssueOfferNotAvailable.errorCode -> CardIssueOfferNotAvailable
                CardIssueInsufficientBalance.errorCode -> CardIssueInsufficientBalance
                CardIssueInvalidShippingAddress.errorCode -> CardIssueInvalidShippingAddress
                CardIssueInvalidEmbossName.errorCode -> CardIssueInvalidEmbossName
                CardCloseLastCard.errorCode -> CardCloseLastCard
                CardActivationInvalidCardData.errorCode -> CardActivationInvalidCardData
                CardActivationCardNotPhysical.errorCode -> CardActivationCardNotPhysical
                CardActivationCardAlreadyActive.errorCode -> CardActivationCardAlreadyActive
                CardActivationCardNotReadyForActivation.errorCode -> CardActivationCardNotReadyForActivation
                CardActivationActiveOrderExists.errorCode -> CardActivationActiveOrderExists
                CardReissuePlasticInvalidSourceCard.errorCode -> CardReissuePlasticInvalidSourceCard
                CardReissuePlasticActiveOrderExists.errorCode -> CardReissuePlasticActiveOrderExists
                CardReissuePlasticInsufficientBalance.errorCode -> CardReissuePlasticInsufficientBalance
                CardReissuePlasticNotAvailable.errorCode -> CardReissuePlasticNotAvailable
                CardReissuePlasticInvalidShippingAddress.errorCode -> CardReissuePlasticInvalidShippingAddress
                CardReissuePlasticInvalidEmbossName.errorCode -> CardReissuePlasticInvalidEmbossName
                BankOfframpUnsupportedConfiguration.errorCode -> BankOfframpUnsupportedConfiguration
                BankOfframpQuoteExpired.errorCode -> BankOfframpQuoteExpired
                BankOfframpRegionUnsupported.errorCode -> BankOfframpRegionUnsupported
                BankOfframpVerificationIncomplete.errorCode -> BankOfframpVerificationIncomplete
                BankOfframpConflict.errorCode -> BankOfframpConflict
                BankOfframpAmountOutOfLimits.errorCode -> BankOfframpAmountOutOfLimits
                BankOfframpRejected.errorCode -> BankOfframpRejected
                else -> Unknown(universalErrorCode)
            }
        }
    }
}