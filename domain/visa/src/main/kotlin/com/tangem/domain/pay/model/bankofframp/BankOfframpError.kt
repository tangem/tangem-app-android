package com.tangem.domain.pay.model.bankofframp

sealed interface BankOfframpError {
    data object Validation : BankOfframpError
    data object RecipientRegionUnsupported : BankOfframpError
    data object SenderRegionUnsupported : BankOfframpError
    data object BusinessUnsupported : BankOfframpError
    data object UnsupportedConfiguration : BankOfframpError
    data object WithdrawalInProgress : BankOfframpError
    data object PollingTimeout : BankOfframpError
    data class Unknown(val cause: Throwable?) : BankOfframpError
}