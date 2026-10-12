package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.converter.Converter

/**
 * Maps a failed bank off-ramp call onto the wizard's error model. The backend names exactly one bank off-ramp
 * failure, so everything else stays unknown rather than being guessed into a screen of its own; [Input.operation]
 * keeps the failing call in the cause, because by the time the failure is logged the call it came from is gone.
 */
internal object BankOfframpErrorConverter : Converter<BankOfframpErrorConverter.Input, BankOfframpError> {

    data class Input(val error: VisaApiError, val operation: String)

    override fun convert(value: Input): BankOfframpError = when (value.error) {
        VisaApiError.BankOfframpUnsupportedConfiguration -> BankOfframpError.UnsupportedConfiguration
        else -> BankOfframpError.Unknown(
            cause = IllegalStateException("Bank off-ramp ${value.operation} failed: ${value.error}"),
            isOutcomeUnknown = value.error.isOutcomeUnknown(),
        )
    }
}