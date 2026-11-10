package com.tangem.domain.tangempay.model.bankofframp

import com.tangem.domain.tangempay.error.TangemPayApiError

sealed interface BankOfframpError {

    val errorCode: Int? get() = null

    data object Validation : BankOfframpError
    data object RecipientRegionUnsupported : BankOfframpError
    data object SenderRegionUnsupported : BankOfframpError
    data object BusinessUnsupported : BankOfframpError
    data object UnsupportedConfiguration : BankOfframpError {
        override val errorCode: Int = TangemPayApiError.BankOfframpUnsupportedConfiguration.errorCode
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