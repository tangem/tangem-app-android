package com.tangem.domain.tangempay.model.bankofframp

import com.tangem.domain.tangempay.error.TangemPayApiError

sealed interface BankOfframpError {

    val errorCode: Int? get() = null

    data object Validation : BankOfframpError
    data object RecipientRegionUnsupported : BankOfframpError
    data object SenderRegionUnsupported : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpRegionUnsupported.errorCode
    }
    data object BusinessUnsupported : BankOfframpError
    data object UnsupportedConfiguration : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpUnsupportedConfiguration.errorCode
    }
    data object QuoteExpired : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpQuoteExpired.errorCode
    }
    data object VerificationIncomplete : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpVerificationIncomplete.errorCode
    }
    data object Conflict : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpConflict.errorCode
    }
    data object AmountOutOfLimits : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpAmountOutOfLimits.errorCode
    }
    data object Rejected : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpRejected.errorCode
    }
    data object WithdrawalInProgress : BankOfframpError
    data object PollingTimeout : BankOfframpError
    data class Unavailable(override val errorCode: Int) : BankOfframpError
    data class Unknown(
        val cause: Throwable?,
        val isOutcomeUnknown: Boolean = false,
        override val errorCode: Int? = null,
    ) : BankOfframpError
}