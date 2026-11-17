package com.tangem.data.tangempay.util

import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.converter.Converter

/**
 * Maps a failed bank off-ramp call onto the wizard's error model. Only the failures the backend names get an error
 * of their own; everything else stays unknown rather than being guessed into a screen of its own. [Input.operation]
 * keeps the failing call in the cause, because by the time the failure is logged the call it came from is gone.
 */
internal object BankOfframpErrorConverter : Converter<BankOfframpErrorConverter.Input, BankOfframpError> {

    data class Input(
        val error: TangemPayApiError,
        val operation: String,
        @RemoveWithToggle(toggleName = "TWI_1778_MONEY_ACCOUNT_MXN_ENABLED") val isMxnEnabled: Boolean,
    )

    @RemoveWithToggle(toggleName = "TWI_1778_MONEY_ACCOUNT_MXN_ENABLED")
    private val MXN_CONTRACT_ERRORS = setOf(
        TangemPayApiError.BankOfframpQuoteExpired,
        TangemPayApiError.BankOfframpRegionUnsupported,
        TangemPayApiError.BankOfframpVerificationIncomplete,
        TangemPayApiError.BankOfframpConflict,
        TangemPayApiError.BankOfframpAmountOutOfLimits,
        TangemPayApiError.BankOfframpRejected,
    )

    override fun convert(value: Input): BankOfframpError {
        if (!value.isMxnEnabled && value.error in MXN_CONTRACT_ERRORS) return unknown(value)
        return when (value.error) {
            TangemPayApiError.BankOfframpUnsupportedConfiguration -> BankOfframpError.UnsupportedConfiguration
            TangemPayApiError.BankOfframpQuoteExpired -> BankOfframpError.QuoteExpired
            TangemPayApiError.BankOfframpRegionUnsupported -> BankOfframpError.SenderRegionUnsupported
            TangemPayApiError.BankOfframpVerificationIncomplete -> BankOfframpError.VerificationIncomplete
            TangemPayApiError.BankOfframpConflict -> BankOfframpError.Conflict
            TangemPayApiError.BankOfframpAmountOutOfLimits -> BankOfframpError.AmountOutOfLimits
            TangemPayApiError.BankOfframpRejected -> BankOfframpError.Rejected
            else -> unknown(value)
        }
    }

    private fun unknown(value: Input) = BankOfframpError.Unknown(
        cause = IllegalStateException("Bank off-ramp ${value.operation} failed: ${value.error}"),
        isOutcomeUnknown = value.error.isOutcomeUnknown(),
        errorCode = value.error.errorCode,
    )
}